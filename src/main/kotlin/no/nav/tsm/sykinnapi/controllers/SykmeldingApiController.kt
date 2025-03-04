package no.nav.tsm.sykinnapi.controllers

import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.service.sykmelding.EksisterendeSykmelding
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingPdfService
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class SykmeldingApiController(
    private val sykmeldingService: SykmeldingService,
    private val sykmeldingPdfService: SykmeldingPdfService,
) {
    @PostMapping("/api/v1/sykmelding/create")
    fun createSykmelding(@RequestBody sykmeldingPayload: SykInnApiNySykmeldingPayload) =
        sykmeldingService.sendSykmelding(sykmeldingPayload)

    @GetMapping("/api/v1/sykmelding/{sykmeldingId}")
    fun getSykmelding(
        @PathVariable sykmeldingId: String,
        @RequestHeader("HPR") hpr: String
    ): EksisterendeSykmelding = sykmeldingService.getSykmelding(sykmeldingId, hpr)

    @GetMapping("/api/v1/sykmelding")
    fun getSykmeldingerByIdent(
        @RequestHeader("Ident") ident: String?
    ): List<EksisterendeSykmelding> {
        if (ident == null) {
            throw IllegalArgumentException("Ident header is missing")
        }

        return sykmeldingService.getSykmeldingerByIdent(ident)
    }

    @GetMapping("/api/v1/sykmelding/{sykmeldingId}/pdf", produces = ["application/pdf"])
    fun getSykmeldingPdf(
        @PathVariable sykmeldingId: String,
        @RequestHeader("HPR") hprnummer: String
    ) = sykmeldingPdfService.getPdf(sykmeldingId, hprnummer)
}
