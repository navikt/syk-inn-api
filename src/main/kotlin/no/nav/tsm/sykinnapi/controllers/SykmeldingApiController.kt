package no.nav.tsm.sykinnapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.service.SykmeldingService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "azuread")
@RestController
class SykmeldingApiController(val sykmeldingService: SykmeldingService) {

    private val securelog = LoggerFactory.getLogger("securelog")

    @PostMapping("/api/v1/sykmelding/create")
    fun createSykmelding(
        @RequestBody sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload,
    ): String {

        securelog.info(
            "sykInnApiNySykmeldingPayload is: ${
                ObjectMapper().writeValueAsString(
                    sykInnApiNySykmeldingPayload,
                )
            }",
        )

        val sykmeldingid = sykmeldingService.create(sykInnApiNySykmeldingPayload)

        return sykmeldingid
    }
}
