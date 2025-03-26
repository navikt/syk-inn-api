package no.nav.tsm.syk_inn_api.controller

import no.nav.tsm.syk_inn_api.model.SykmeldingDTO
import no.nav.tsm.syk_inn_api.model.SykmeldingPayload
import no.nav.tsm.syk_inn_api.service.SykmeldingPdfService
import no.nav.tsm.syk_inn_api.service.SykmeldingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/ut/sykmelding/")
class SykmeldingController(
    private val sykmeldingService: SykmeldingService,
    private val sykmeldingPdfService: SykmeldingPdfService,
) {

    @PostMapping("/")
    fun opprettSykmelding(@RequestBody payload: SykmeldingPayload): ResponseEntity<String> {
        if (payload.pasientFnr.isBlank() || payload.sykmelderHpr.isBlank()) {
            return ResponseEntity.badRequest().body("Pasient fnr and sykmelder hpr are required")
        }

        return sykmeldingService.createSykmelding(payload)
    }

    @GetMapping("/{sykmeldingId}")
    fun getSykmeldingById(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String
    ): ResponseEntity<Any> {
        if (hpr.isBlank()) {
            return ResponseEntity.badRequest().body("HPR header is missing")
        }
        return ResponseEntity(
            sykmeldingService.getSykmeldingById(sykmeldingId, hpr),
            org.springframework.http.HttpStatus.OK,
        )

    }

    @GetMapping("/")
    fun getSykmeldingerByUserIdent(
        @RequestHeader("Ident") ident: String?
    ): ResponseEntity<Any> {
        if (ident == null) {
            return ResponseEntity.badRequest().body("Ident header is missing")
        }
        return ResponseEntity(
            sykmeldingService.getSykmeldingerByIdent(ident),
            org.springframework.http.HttpStatus.OK,
        )
    }

    @GetMapping("/{sykmeldingId}/pdf", produces = ["application/pdf"])
    fun getSykmeldingPdf(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String
    ): ResponseEntity<Any> {
        if (hpr.isBlank()) {
            return ResponseEntity.badRequest().body("HPR header is missing")
        }
        return ResponseEntity(
            sykmeldingPdfService.getPdf(sykmeldingId.toString(), hpr),
            org.springframework.http.HttpStatus.OK,
        )
    }
}
