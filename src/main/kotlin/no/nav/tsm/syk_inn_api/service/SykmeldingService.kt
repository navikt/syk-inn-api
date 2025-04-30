package no.nav.tsm.syk_inn_api.service

import java.util.*
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.kafka.KafkaStubber
import no.nav.tsm.syk_inn_api.model.sykmelding.SavedSykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingEntity
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.repository.SykmeldingRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val ruleService: RuleService,
    private val helsenettProxyService: HelsenettProxyService,
) {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    fun createSykmelding(payload: SykmeldingPayload): ResponseEntity<Any> {
        val sykmeldingId = UUID.randomUUID().toString()
        val sykmelder = helsenettProxyService.getSykmelderByHpr(payload.sykmelderHpr, sykmeldingId)

        val ruleResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder,
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

        val entity =
            sykmeldingRepository.save(
                mapToEntity(
                    payload = payload,
                    sykmeldingId = sykmeldingId,
                ),
            )
        if (entity.id == null) {
            logger.info("Lagring av sykmelding with id=$sykmeldingId er feilet")
            return ResponseEntity.internalServerError().body("Lagring av sykmelding feilet")
        }
        logger.info("Sykmelding with id=$sykmeldingId er lagret")

        // send på kafka
        // TODO implement
        val kafkaResponse = KafkaStubber().sendToOpprettSykmeldingTopic(payload)
        if (!kafkaResponse) {
            return ResponseEntity.internalServerError()
                .body("Sending av sykmelding på kafka topic: opprett-sykmelding-w/e feilet")
        }

        // svar om sending på kafka er ok
        return ResponseEntity.status(HttpStatus.CREATED).body("Sykmeldingen er lagret")
    }

    private fun mapToEntity(payload: SykmeldingPayload, sykmeldingId: String): SykmeldingEntity {
        logger.info("Mapping sykmelding til entity")
        return SykmeldingEntity(
            sykmeldingId = sykmeldingId,
            pasientFnr = payload.pasientFnr,
            sykmelderHpr = payload.sykmelderHpr,
            sykmelding = payload.sykmelding,
            legekontorOrgnr = payload.legekontorOrgnr,
        )
    }

    fun getSykmeldingById(sykmeldingId: UUID, hpr: String): ResponseEntity<Any> {
        val sykmelding =
            sykmeldingRepository.findSykmeldingEntityBySykmeldingId(sykmeldingId.toString())
                ?: return ResponseEntity.notFound().build()

        return ResponseEntity.ok(
            mapToSavedSykmelding(sykmelding),
        )
    }

    private fun mapToSavedSykmelding(sykmelding: SykmeldingEntity): SavedSykmelding {
        return SavedSykmelding(
            sykmeldingId = sykmelding.sykmeldingId,
            pasientFnr = sykmelding.pasientFnr,
            sykmelderHpr = sykmelding.sykmelderHpr,
            sykmelding = sykmelding.sykmelding,
            legekontorOrgnr = sykmelding.legekontorOrgnr,
        )
    }

    fun getSykmeldingerByIdent(ident: String): ResponseEntity<Any> {
        val sykmeldinger = sykmeldingRepository.findSykmeldingEntitiesByPasientFnr(ident)

        if (sykmeldinger.isEmpty()) {
            return ResponseEntity.noContent().build()
        }

        return ResponseEntity.ok(
            sykmeldinger.map {
                SavedSykmelding(
                    sykmeldingId = it.sykmeldingId,
                    pasientFnr = it.pasientFnr,
                    sykmelderHpr = it.sykmelderHpr,
                    sykmelding = it.sykmelding,
                    legekontorOrgnr = it.legekontorOrgnr,
                )
            },
        )
    }
}
