package no.nav.tsm.syk_inn_api.sykmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.raise.result
import arrow.core.right
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.btsys.BtsysService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HelsenettProxyService
import no.nav.tsm.syk_inn_api.sykmelding.kafka.SykmeldingKafkaService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.secureLogger
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    private val ruleService: RuleService,
    private val helsenettProxyService: HelsenettProxyService,
    private val btsysService: BtsysService,
    private val sykmeldingKafkaService: SykmeldingKafkaService,
    private val personService: PersonService,
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
) {
    private val logger = logger()
    private val securelog = secureLogger()

    enum class SykmeldingCreationErrors {
        RULE_VALIDATION,
        PERSISTENCE_ERROR,
        RESOURCE_ERROR
    }

    fun createSykmelding(
        payload: OpprettSykmeldingPayload
    ): Either<SykmeldingCreationErrors, SykmeldingDocument> {
        val sykmeldingId = UUID.randomUUID().toString()
        val mottatt = OffsetDateTime.now(ZoneOffset.UTC)

        val resources = result {
            val person = personService.getPersonByIdent(payload.meta.pasientIdent).bind()
            val sykmelder =
                helsenettProxyService
                    .getSykmelderByHpr(payload.meta.sykmelderHpr, sykmeldingId)
                    .bind()
            val sykmelderSuspendert =
                btsysService
                    .isSuspended(
                        sykmelderFnr = sykmelder.fnr,
                        signaturDato = LocalDate.now().toString(),
                    )
                    .bind()

            Triple(person, sykmelder, sykmelderSuspendert)
        }

        val (person, sykmelder, sykmelderSuspendert) =
            resources.fold(
                { it },
                {
                    logger.error("Feil ved henting av eksterne ressurser: $it")
                    return SykmeldingCreationErrors.RESOURCE_ERROR.left()
                },
            )

        val ruleResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder,
                sykmelderSuspendert = sykmelderSuspendert,
                foedselsdato = person.fodselsdato,
            )

        if (ruleResult.status != RegulaStatus.OK) {
            return SykmeldingCreationErrors.RULE_VALIDATION.left()
        }

        val sykmeldingResponse =
            sykmeldingPersistenceService.saveSykmeldingPayload(
                sykmeldingId = sykmeldingId,
                mottatt = mottatt,
                payload = payload,
                person = person,
                sykmelder = sykmelder,
                ruleResult = ruleResult,
            )

        if (sykmeldingResponse == null) {
            logger.info("Lagring av sykmelding with id=$sykmeldingId er feilet")
            return SykmeldingCreationErrors.PERSISTENCE_ERROR.left()
        }

        sykmeldingKafkaService.send(
            sykmeldingId = sykmeldingId,
            sykmelding = sykmeldingResponse,
            person = person,
            sykmelder = sykmelder,
            regulaResult = ruleResult,
        )

        return sykmeldingResponse.right()
    }

    // TODO: Faktisk implementer hpr-tilgangsstyring
    fun getSykmeldingById(sykmeldingId: UUID, hpr: String): SykmeldingDocument? =
        sykmeldingPersistenceService.getSykmeldingById(sykmeldingId.toString())

    fun getSykmeldingerByIdent(ident: String, orgnr: String): Result<List<SykmeldingDocument>> {
        securelog.info("Henter sykmeldinger for ident=$ident")
        // TODO bør vi ha en kul sjekk på om lege har en tilknytning til gitt legekontor orgnr slik
        // at den får lov til å sjå ?
        val sykmeldinger: List<SykmeldingDocument> =
            sykmeldingPersistenceService.getSykmeldingerByIdent(ident).filter {
                it.meta.legekontorOrgnr == orgnr
            }

        if (sykmeldinger.isEmpty()) {
            return Result.success(emptyList())
        }

        return Result.success(sykmeldinger)
    }
}
