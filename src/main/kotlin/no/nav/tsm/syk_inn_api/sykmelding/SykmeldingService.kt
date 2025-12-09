package no.nav.tsm.syk_inn_api.sykmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.result
import arrow.core.right
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingKafkaMapper
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import no.nav.tsm.syk_inn_api.utils.failSpan
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*

@Service
class SykmeldingService(
    private val ruleService: RuleService,
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    private val sykmeldingInputProducer: SykmeldingInputProducer,
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    sealed class SykmeldingCreationErrors {
        data object PersonDoesNotExist : SykmeldingCreationErrors()

        data object ProcessingError : SykmeldingCreationErrors()

        data object PersistenceError : SykmeldingCreationErrors()

        data object ResourceError : SykmeldingCreationErrors()

        data class AlreadyExists(val sykmeldingDocument: SykmeldingDocument) :
            SykmeldingCreationErrors()
    }

    @WithSpan
    fun createSykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, SykmeldingDocument> {
        val span = Span.current()
        val sykmeldingId = UUID.randomUUID().toString()
        val mottatt = OffsetDateTime.now(ZoneOffset.UTC)
        val existing = sykmeldingPersistenceService.getSykmeldingByIdempotencyKey(payload.submitId)
        if (existing != null) {
            return duplicateSubmitResult(payload, existing)
        }

        val resources = result {
            val person = personService.getPersonByIdent(payload.meta.pasientIdent).bind()
            val sykmelder =
                sykmelderService
                    .sykmelderMedSuspensjon(
                        hpr = payload.meta.sykmelderHpr,
                        signaturDato = mottatt.toLocalDate(),
                        callId = sykmeldingId,
                    )
                    .bind()

            person to sykmelder
        }

        val (person, sykmelder) =
            resources.fold(
                { it },
                {
                    it.failSpan()
                    logger.error("Feil ved henting av eksterne ressurser: $it")
                    return SykmeldingCreationErrors.ResourceError.left()
                },
            )

        val ruleResult: RegulaResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder,
                foedselsdato = person.fodselsdato,
            )

        val validation = SykmeldingKafkaMapper.mapValidationResult(ruleResult)

        try {
            val sykmeldingDocument =
                sykmeldingPersistenceService.saveSykmeldingPayload(
                    sykmeldingId = sykmeldingId,
                    mottatt = mottatt,
                    payload = payload,
                    person = person,
                    sykmelder = sykmelder,
                    ruleResult = validation,

                )
            sykmeldingProducer.send(
                sykmeldingId = sykmeldingId,
                sykmelding = sykmeldingDocument,
                person = person,
                sykmelder = sykmelder,
                validationResult = validation,
                source = payload.meta.source,
            )

            span.setAttribute("SykmeldingService.create.sykmeldingId", sykmeldingId)
            span.setAttribute("SykmeldingService.create.source", payload.meta.source)
            logger.info("Created and sent sykmelding with id=$sykmeldingId to Kafka")

            return sykmeldingDocument.right()
        } catch (ex: DataIntegrityViolationException) {
            logger.warn(
                "Sykmelding med submitId=${payload.submitId} allerede er lagret i databasen",
                ex
            )
            return duplicateSubmitResult(
                payload = payload,
                existing =
                    sykmeldingPersistenceService.getSykmeldingByIdempotencyKey(payload.submitId)
            )
        }
    }

    private fun duplicateSubmitResult(
        payload: OpprettSykmeldingPayload,
        existing: SykmeldingDocument?
    ): Either<SykmeldingCreationErrors, Nothing> {
        logger.warn(
            "Sykmelding med submitId=${payload.submitId} allerede er lagret i databasen",
        )

        existing ?: return SykmeldingCreationErrors.ProcessingError.left()

        val pasientId = existing.meta.pasientIdent
        val hprNr = existing.meta.sykmelder.hprNummer

        if (hprNr != payload.meta.sykmelderHpr) {
            return SykmeldingCreationErrors.ProcessingError.left()
        } else if (pasientId != payload.meta.pasientIdent) {
            return SykmeldingCreationErrors.ProcessingError.left()
        }

        return SykmeldingCreationErrors.AlreadyExists(existing).left()
    }

    @WithSpan
    fun getSykmeldingById(sykmeldingId: UUID): SykmeldingDocument? =
        sykmeldingPersistenceService.getSykmeldingById(sykmeldingId.toString())

    @WithSpan
    fun getSykmeldingerByIdent(ident: String): Result<List<SykmeldingDocument>> {
        teamLogger.info("Henter sykmeldinger for ident=$ident")

        val sykmeldinger: List<SykmeldingDocument> =
            sykmeldingPersistenceService.getSykmeldingerByIdent(ident)

        if (sykmeldinger.isEmpty()) {
            return Result.success(emptyList())
        }

        return Result.success(sykmeldinger)
    }

    @WithSpan
    fun verifySykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, RegulaResult> {
        val sykmeldingId = UUID.randomUUID().toString()
        val mottatt = OffsetDateTime.now(ZoneOffset.UTC)

        val person: Person =
            personService.getPersonByIdent(payload.meta.pasientIdent).fold({ it }) {
                return SykmeldingCreationErrors.PersonDoesNotExist.left()
            }

        val sykmelder =
            sykmelderService
                .sykmelderMedSuspensjon(
                    hpr = payload.meta.sykmelderHpr,
                    signaturDato = mottatt.toLocalDate(),
                    callId = sykmeldingId,
                )
                .fold({ it }) {
                    logger.error(
                        "Feil ved henting av sykmelder med hpr=${payload.meta.sykmelderHpr}"
                    )
                    it.failSpan()
                    return SykmeldingCreationErrors.ResourceError.left()
                }

        val ruleResult: RegulaResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder,
                foedselsdato = person.fodselsdato,
            )

        return ruleResult.right()
    }
}
