package no.nav.tsm.sykinnapi.controllers

import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.service.sykmeldingInnsending.SykmeldingInnsending
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@RestController
class SykmeldingApiController(val sykmeldingInnsending: SykmeldingInnsending) {
    @PostMapping("/api/v1/sykmelding/create")
    fun createSykmelding(
        @RequestBody sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload,
    ): String {

        return sykmeldingInnsending.send(sykInnApiNySykmeldingPayload)
    }
}

class MissingDataException(override val message: String) : Exception(message)
