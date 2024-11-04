package no.nav.tsm.sykinnapi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SykmeldingApiController {
    private val logger = LoggerFactory.getLogger(SykmeldingApiController::class.java)

    @PostMapping("/api/v1/sykmelding/create")
    fun createSykmelding(
        @RequestBody sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload,
    ): String {
        logger.info(
            "All good in the hood sykmelding doc: ${
                ObjectMapper().writeValueAsString(
                    sykInnApiNySykmeldingPayload,
                )
            }",
        )
        return "ok"
    }
}
