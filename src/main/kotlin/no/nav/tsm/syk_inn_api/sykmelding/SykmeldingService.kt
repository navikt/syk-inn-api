package no.nav.tsm.syk_inn_api.sykmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.result
import arrow.core.right
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingInputProducer
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.secureLogger
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    private val ruleService: RuleService,
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    private val sykmeldingInputProducer: SykmeldingInputProducer,
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
) {
    private val logger = logger()
    private val securelog = secureLogger()

    sealed class SykmeldingCreationErrors {
        data class RuleValidation(val result: RegulaResult.NotOk) : SykmeldingCreationErrors()

        object PersistenceError : SykmeldingCreationErrors()

        object ResourceError : SykmeldingCreationErrors()
    }

    fun createSykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, SykmeldingDocument> {
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

        if (ruleResult is RegulaResult.NotOk) {
            return SykmeldingCreationErrors.RuleValidation(ruleResult).left()
        }

        val sykmeldingDocument =
            sykmeldingPersistenceService.saveSykmeldingPayload(
                sykmeldingId = sykmeldingId,
                mottatt = mottatt,
                payload = payload,
                person = person,
                sykmelder = sykmelder,
                ruleResult = ruleResult,
            )

        if (sykmeldingDocument == null) {
            logger.info("Lagring av sykmelding with id=$sykmeldingId er feilet")
            return SykmeldingCreationErrors.PersistenceError.left()
        }

        sykmeldingInputProducer.send(
            sykmeldingId = sykmeldingId,
            sykmelding = sykmeldingDocument,
            person = person,
            sykmelder = sykmelder,
            regulaResult = ruleResult,
        )

        return sykmeldingDocument.right()
    }

    fun getSykmeldingById(
        sykmeldingId: UUID,
        // TODO: Faktisk implementer hpr-tilgangsstyring
        hpr: String
    ): SykmeldingDocument? = sykmeldingPersistenceService.getSykmeldingById(sykmeldingId.toString())

    fun getSykmeldingerByIdent(
        ident: String,
        // TODO: Faktisk implementer hpr-tilgangsstyring
        hpr: String
    ): Result<List<SykmeldingDocument>> {
        securelog.info("Henter sykmeldinger for ident=$ident")

        val sykmeldinger: List<SykmeldingDocument> =
            sykmeldingPersistenceService.getSykmeldingerByIdent(ident)

        if (sykmeldinger.isEmpty()) {
            return Result.success(emptyList())
        }

        return Result.success(sykmeldinger)
    }
}
