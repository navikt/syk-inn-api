package no.nav.tsm.sykinnapi.controllers

import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.service.sykmeldingHent.SykmeldingHent
import no.nav.tsm.sykinnapi.service.sykmeldingInnsending.SykmeldingInnsending
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class SykmeldingApiController(
    val sykmeldingInnsending: SykmeldingInnsending,
    val sykmeldingHent: SykmeldingHent
) {
    @PostMapping("/api/v1/sykmelding/create")
    fun createSykmelding(@RequestBody sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload) =
        sykmeldingInnsending.send(sykInnApiNySykmeldingPayload)

    @GetMapping("/api/v1/sykmelding/{sykmeldingId}")
    fun getSykmelding(
        @PathVariable sykmeldingId: String,
        @RequestHeader("hprnummer") hprnummer: String
    ) = sykmeldingHent.get(sykmeldingId, hprnummer)
}
