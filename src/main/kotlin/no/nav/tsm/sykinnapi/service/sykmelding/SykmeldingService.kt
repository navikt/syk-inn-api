package no.nav.tsm.sykinnapi.service.sykmelding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.sykinnapi.mapper.receivedSykmeldingWithValidationMapper
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingService {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    fun create(
        sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload,
        sykmelderFnr: String,
        sykmeldingId: String
    ): String {

        val receivedSykmeldingWithValidation =
            receivedSykmeldingWithValidationMapper(
                sykInnApiNySykmeldingPayload,
                sykmelderFnr,
                sykmeldingId
            )

        logger.info(
            "Successfully created receivedSykmeldingWithValidation: ${
                ObjectMapper().apply {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                }.writeValueAsString(receivedSykmeldingWithValidation)}"
        )

        // TODO put receivedSykmeldingWithValidationResult on kafka topic
        // TODO Do rule check here, api? regilus-maximus, or call to syfosmregler?

        return receivedSykmeldingWithValidation.sykmelding.id
    }
}
