package no.nav.tsm.syk_inn_api.sykmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.result
import arrow.core.right
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingKafkaMapper
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingProducer
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import no.nav.tsm.syk_inn_api.utils.failSpan
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SykmeldingService(
    private val ruleService: RuleService,
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    private val sykmeldingInputProducer: SykmeldingProducer,
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    sealed class SykmeldingCreationErrors {
        object PersonDoesNotExist : SykmeldingCreationErrors()

        object PersistenceError : SykmeldingCreationErrors()

        object ResourceError : SykmeldingCreationErrors()
    }

    @Transactional
    @WithSpan
    fun createSykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, SykmeldingDocument> {
        val span = Span.current()
        val sykmeldingId = UUID.randomUUID().toString()
        val mottatt = OffsetDateTime.now(ZoneOffset.UTC)

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
        val sykmeldingDocument =
            sykmeldingPersistenceService.saveSykmeldingPayload(
                sykmeldingId = sykmeldingId,
                mottatt = mottatt,
                payload = payload,
                person = person,
                sykmelder = sykmelder,
                ruleResult = validation,
            )

        sykmeldingInputProducer.send(
            sykmeldingId = sykmeldingId,
            sykmelding = sykmeldingDocument,
            person = person,
            sykmelder = sykmelder,
            validationResult = validation,
            source = payload.meta.source,
        )

        span.setAttribute("SykmeldingService.create.sykmeldingId", sykmeldingId)
        span.setAttribute("SykmeldingService.create.source", payload.meta.source)

        return sykmeldingDocument.right()
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
