package no.nav.tsm.syk_inn_api.service

import java.util.*
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.model.sykmelding.SavedSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingDb
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.model.sykmelding.fromPGobject
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
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

    fun createSykmelding(payload: SykmeldingPayload): ResponseEntity<Any> {
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
            return ResponseEntity.badRequest().body(ruleResult.status)
        }
        logger.info(
            "Sykmelding med id=$sykmeldingId er validert mot regler med status=${ruleResult.status}",
        )

        val entity = sykmeldingPersistenceService.save(payload, sykmeldingId)

        if (entity.id == null) {
            logger.info("Lagring av sykmelding with id=$sykmeldingId er feilet")
            return ResponseEntity.internalServerError().body("Lagring av sykmelding feilet")
        }
        logger.info("Sykmelding with id=$sykmeldingId er lagret")

        sykmeldingKafkaService.send(payload, sykmeldingId, pdlPerson, sykmelder, ruleResult)
        return ResponseEntity.status(HttpStatus.CREATED).body("Sykmeldingen er lagret")
    }

    fun getSykmeldingById(sykmeldingId: UUID, hpr: String): ResponseEntity<Any> {
        val sykmelding =
            sykmeldingPersistenceService.getSykmeldingById(sykmeldingId.toString())
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            mapToSavedSykmelding(sykmelding),
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

    fun getSykmeldingerByIdent(ident: String): ResponseEntity<Any> {
        logger.info("Henter sykmeldinger for ident=$ident")
        val sykmeldinger = sykmeldingPersistenceService.getSykmeldingerByIdent(ident)

        if (sykmeldinger.isEmpty()) {
            return ResponseEntity.ok(emptyList<SavedSykmelding>())
        }

        return ResponseEntity.ok(
            sykmeldinger.map {
                SavedSykmelding( // TODO this should have all the fields needed in the dashboard,
                    // wait with aareg.
                    sykmeldingId = it.sykmeldingId,
                    pasientFnr = it.pasientFnr,
                    sykmelderHpr = it.sykmelderHpr,
                    sykmelding = it.sykmelding.fromPGobject(),
                    legekontorOrgnr = it.legekontorOrgnr,
                )
            },
        )
    }
}
