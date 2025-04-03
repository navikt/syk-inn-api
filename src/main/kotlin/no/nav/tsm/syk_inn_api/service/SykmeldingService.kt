package no.nav.tsm.syk_inn_api.service

import java.util.*
import no.nav.tsm.syk_inn_api.kafka.KafkaStubber
import no.nav.tsm.syk_inn_api.model.SavedSykmelding
import no.nav.tsm.syk_inn_api.model.SykmeldingDTO
import no.nav.tsm.syk_inn_api.model.SykmeldingPayload
import no.nav.tsm.syk_inn_api.repository.SykmeldingRepository
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    private val sykmeldingRepository: SykmeldingRepository,
    private val ruleService: RuleService,
    private val helsenettProxyService: HelseNettProxyService,
) {
    fun createSykmelding(payload: SykmeldingPayload): ResponseEntity<String> {
        val sykmeldingId = UUID.randomUUID().toString()
        val sykmelder = helsenettProxyService.getSykmelderByHpr(payload.sykmelderHpr, sykmeldingId)

        // valider mot regler
        val ruleResult =
            ruleService.validateRules(
                payload = payload,
                sykmeldingId = sykmeldingId,
                sykmelder = sykmelder
            )
        if (!ruleResult) {
            return ResponseEntity.badRequest().body("Rule validation failed")
        }

        // save payload
        //        sykmeldingRepository.save(payload)
        // TODO implement
        val savedPayload = repositorySaveStub(payload)
        if (savedPayload.id == null) {
            return ResponseEntity.internalServerError().body("Lagring av sykmelding feilet")
        }

        // send på kafka
        val kafkaResponse = KafkaStubber().sendToOpprettSykmeldingTopic(payload)
        if (!kafkaResponse) {
            return ResponseEntity.internalServerError()
                .body("Sending av sykmelding på kafka topic: opprett-sykmelding-w/e feilet")
        }

        // svar om sending på kafka er ok
        return ResponseEntity.status(HttpStatus.CREATED).body("Sykmeldingen er lagret")
    }

    fun getSykmeldingById(sykmeldingId: UUID, hpr: String): ResponseEntity<Any> {
        //        val sykmelding = sykmeldingRepository.findById(sykmeldingId)
        // TODO implement
        val sykmelding = repositoryGetByIdStub(sykmeldingId)
        if (sykmelding.isEmpty) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(
            SavedSykmelding(
                id = sykmeldingId.toString(),
                fnr = sykmelding.get().fnr,
            ),
        )
    }

    fun getSykmeldingerByIdent(ident: String): ResponseEntity<Any> {
        // TODO implement
        val sykmeldinger = repositoryGetByIdentStub(ident)
        if (sykmeldinger.isEmpty()) {
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(
            sykmeldinger.map {
                SavedSykmelding(
                    id = it.id,
                    fnr = it.fnr,
                )
            },
        )
    }

    fun repositorySaveStub(payload: SykmeldingPayload): SykmeldingDTO {
        return SykmeldingDTO(id = UUID.randomUUID().toString(), fnr = payload.pasientFnr)
    }

    fun repositoryGetByIdStub(id: UUID): Optional<SykmeldingDTO> {
        return Optional.of(SykmeldingDTO(id = id.toString(), fnr = "12345678901"))
    }

    fun repositoryGetByIdentStub(ident: String): List<SykmeldingDTO> {
        return listOf(SykmeldingDTO(id = UUID.randomUUID().toString(), fnr = "12345678901"))
    }
}
