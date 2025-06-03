package no.nav.tsm.syk_inn_api.sykmelding

import java.time.LocalDate
import java.util.*
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.model.SykmeldingResult
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.btsys.BtsysService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HelsenettProxyService
import no.nav.tsm.syk_inn_api.sykmelding.kafka.SykmeldingKafkaService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
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
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    fun createSykmelding(payload: SykmeldingPayload): SykmeldingResult {
        val sykmeldingId = UUID.randomUUID().toString()
        val person = personService.getPersonByIdent(payload.pasientFnr)
        val sykmelder =
            helsenettProxyService
                .getSykmelderByHpr(
                    payload.sykmelderHpr,
                    sykmeldingId,
                )
                .getOrThrow()
        val sykmelderSuspendert =
            btsysService
                .isSuspended(
                    sykmelderFnr = sykmelder.fnr,
                    signaturDato = LocalDate.now().toString(),
                )
                .getOrThrow()

        requireNotNull(person.fodselsdato) {
            "Person with ident=${payload.pasientFnr} does not have a valid fødselsdato"
        }

        val ruleResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder,
                sykmelderSuspendert = sykmelderSuspendert,
                foedselsdato = person.fodselsdato,
            )
        if (ruleResult.status != RegulaStatus.OK) {
            logger.error(
                "Sykmelding med id=$sykmeldingId er feilet validering mot regler med status=${ruleResult.status}",
            )
            return SykmeldingResult.Failure(
                errorMessage = "Bad request ved regelvalidering: ${ruleResult.status}",
                errorCode = HttpStatus.BAD_REQUEST,
            )
        }
        logger.info(
            "Sykmelding med id=$sykmeldingId er validert mot regler med status=${ruleResult.status}",
        )

        val sykmeldingResponse =
            sykmeldingPersistenceService.saveSykmeldingPayload(payload, sykmeldingId)

        if (sykmeldingResponse == null) {
            logger.info("Lagring av sykmelding with id=$sykmeldingId er feilet")
            return SykmeldingResult.Failure(
                errorMessage = "Internal server error ved lagring av sykmelding",
                errorCode = HttpStatus.INTERNAL_SERVER_ERROR,
            )
        }
        logger.info("Sykmelding with id=$sykmeldingId er lagret")

        sykmeldingKafkaService.send(payload, sykmeldingId, person, sykmelder, ruleResult)
        return SykmeldingResult.Success(
            statusCode = HttpStatus.CREATED,
            sykmeldingResponse = sykmeldingResponse,
        )
    }

    fun getSykmeldingById(sykmeldingId: UUID, hpr: String): SykmeldingResult {
        val sykmeldingResponse =
            sykmeldingPersistenceService.getSykmeldingById(sykmeldingId.toString())
                ?: return SykmeldingResult.Failure(
                    errorMessage = "Sykmelding not found for sykmeldingId=$sykmeldingId",
                    errorCode = HttpStatus.NOT_FOUND,
                )

        return SykmeldingResult.Success(
            sykmeldingResponse = sykmeldingResponse,
            statusCode = HttpStatus.OK,
        )
    }

    fun getSykmeldingerByIdent(ident: String, orgnr: String): SykmeldingResult {
        logger.info("Henter sykmeldinger for ident=$ident")
        // TODO bør vi ha en kul sjekk på om lege har en tilknytning til gitt legekontor orgnr slik
        // at den får lov til å sjå ?
        val sykmeldingResponses =
            sykmeldingPersistenceService.getSykmeldingerByIdent(ident).filter {
                it.legekontorOrgnr == orgnr
            }

        if (sykmeldingResponses.isEmpty()) {
            return SykmeldingResult.Success(
                sykmeldinger = emptyList(),
                statusCode = HttpStatus.NO_CONTENT,
            )
        }

        return SykmeldingResult.Success(
            sykmeldinger = sykmeldingResponses,
            statusCode = HttpStatus.OK,
        )
    }
}
