package no.nav.tsm.sykinnapi.controllers

import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingHistorikk
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingKvittering
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class SykmeldingApiController(
    val sykmeldingService: SykmeldingService,
) {
    @PostMapping("/api/v1/sykmelding/create")
    fun createSykmelding(@RequestBody sykmeldingPayload: SykInnApiNySykmeldingPayload) =
        sykmeldingService.sendSykmelding(sykmeldingPayload)

    @GetMapping("/api/v1/sykmelding/{sykmeldingId}")
    fun getSykmeldingKvittering(
        @PathVariable sykmeldingId: String,
        @RequestHeader("HPR") hprnummer: String
    ): SykmeldingKvittering {
        return sykmeldingService.getSykmeldingKvittering(sykmeldingId, hprnummer)
    }

    @GetMapping("/api/v1/sykmelding")
    fun getSykmeldingByIdent(@RequestHeader("Ident") ident: String?): List<SykmeldingHistorikk> {
        if (ident == null) {
            throw IllegalArgumentException("Ident header is missing")
        }

        return sykmeldingService.getSykmeldingerByIdent(ident)
    }
}
