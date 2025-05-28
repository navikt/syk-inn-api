package no.nav.tsm.syk_inn_api.service

import java.util.*
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.model.SykmeldingResult
import no.nav.tsm.syk_inn_api.model.sykmelding.SavedSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingDb
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.model.sykmelding.fromPGobject
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    private val ruleService: RuleService,
    private val helsenettProxyService: HelsenettProxyService,
    private val sykmeldingKafkaService: SykmeldingKafkaService,
    private val pdlService: PdlService,
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
) {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    fun createSykmelding(payload: SykmeldingPayload): SykmeldingResult {
        val sykmeldingId = UUID.randomUUID().toString()
        val sykmelder = helsenettProxyService.getSykmelderByHpr(payload.sykmelderHpr, sykmeldingId)
        val pdlPerson = pdlService.getPdlPerson(payload.pasientFnr)
        val foedselsdato = pdlPerson.foedselsdato
        requireNotNull(foedselsdato)

        val ruleResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder,
                foedselsdato = foedselsdato,
            )
        if (ruleResult.status != RegulaStatus.OK) {
            logger.info(
                "Sykmelding med id=$sykmeldingId er feilet validering mot regler med status=${ruleResult.status}",
            )
            return SykmeldingResult.Failure(
                errorMessage = "Bad request ved regelvalidering: ${ruleResult.status}",
                errorCode = HttpStatus.BAD_REQUEST
            )
        }
        logger.info(
            "Sykmelding med id=$sykmeldingId er validert mot regler med status=${ruleResult.status}",
        )

        val entity = sykmeldingPersistenceService.save(payload, sykmeldingId)

        if (entity.id == null) {
            logger.info("Lagring av sykmelding with id=$sykmeldingId er feilet")
            return SykmeldingResult.Failure(
                errorMessage = "Internal server error ved lagring av sykmelding",
                errorCode = HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
        logger.info("Sykmelding with id=$sykmeldingId er lagret")

        sykmeldingKafkaService.send(payload, sykmeldingId, pdlPerson, sykmelder, ruleResult)
        return SykmeldingResult.Success(statusCode = HttpStatus.CREATED)
    }

    fun getSykmeldingById(sykmeldingId: UUID, hpr: String): SykmeldingResult {
        val sykmelding =
            sykmeldingPersistenceService.getSykmeldingById(sykmeldingId.toString())
                ?: return SykmeldingResult.Failure(
                    errorMessage = "Sykmelding not found for sykmeldingId=$sykmeldingId",
                    errorCode = HttpStatus.NOT_FOUND
                )

        return SykmeldingResult.Success(
            savedSykmelding = mapToSavedSykmelding(sykmelding),
            statusCode = HttpStatus.OK
        )
    }

    private fun mapToSavedSykmelding(sykmelding: SykmeldingDb): SavedSykmelding {
        return SavedSykmelding(
            sykmeldingId = sykmelding.sykmeldingId,
            pasientFnr = sykmelding.pasientFnr,
            sykmelderHpr = sykmelding.sykmelderHpr,
            sykmelding = sykmelding.sykmelding.fromPGobject(),
            legekontorOrgnr = sykmelding.legekontorOrgnr,
        )
    }

    fun getSykmeldingerByIdent(ident: String, orgnr: String): SykmeldingResult {
        logger.info("Henter sykmeldinger for ident=$ident")
        // TODO bør vi ha en kul sjekk på om lege har en tilknytning til gitt legekontor orgnr slik
        // at den får lov til å sjå ?
        val sykmeldinger =
            sykmeldingPersistenceService.getSykmeldingerByIdent(ident).filter {
                it.legekontorOrgnr == orgnr
            }

        if (sykmeldinger.isEmpty()) {
            return SykmeldingResult.Success(
                sykmeldinger = emptyList(),
                statusCode = HttpStatus.NO_CONTENT
            )
        }

        return SykmeldingResult.Success(
            sykmeldinger = sykmeldinger.map { mapToSavedSykmelding(it) },
            statusCode = HttpStatus.OK
        )
    }
}
