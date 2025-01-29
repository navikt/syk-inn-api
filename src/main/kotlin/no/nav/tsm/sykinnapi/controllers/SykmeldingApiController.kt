package no.nav.tsm.sykinnapi.controllers

import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.service.sykmeldingHent.SykmeldingKvittering
import no.nav.tsm.sykinnapi.service.sykmeldingHent.SykmeldingKvitteringService
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
    val sykmeldingKvittering: SykmeldingKvitteringService
) {
    @PostMapping("/api/v1/sykmelding/create")
    fun createSykmelding(@RequestBody sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload) =
        sykmeldingInnsending.send(sykInnApiNySykmeldingPayload)

    @GetMapping("/api/v1/sykmelding/{sykmeldingId}")
    fun getSykmeldingKvittering(
        @PathVariable sykmeldingId: String,
        @RequestHeader("X-HPR") hprnummer: String
    ): SykmeldingKvittering {
        return sykmeldingKvittering.get(sykmeldingId, hprnummer)
    }
}
