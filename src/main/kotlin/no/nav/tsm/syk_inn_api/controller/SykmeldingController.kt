package no.nav.tsm.syk_inn_api.controller

import jakarta.servlet.http.HttpServletRequest
import java.util.*
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.service.SykmeldingPdfService
import no.nav.tsm.syk_inn_api.service.SykmeldingService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sykmelding")
class SykmeldingController(
    private val sykmeldingService: SykmeldingService,
    private val sykmeldingPdfService: SykmeldingPdfService,
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createSykmelding(@RequestBody payload: SykmeldingPayload): ResponseEntity<Any> {
        if (payload.pasientFnr.isBlank() || payload.sykmelderHpr.isBlank()) {
            return ResponseEntity.badRequest().body("Pasient fnr and sykmelder hpr are required")
        }

        return sykmeldingService.createSykmelding(payload)
    }

    @GetMapping("/{sykmeldingId}")
    fun getSykmeldingById(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        return ResponseEntity(
            sykmeldingService.getSykmeldingById(sykmeldingId, hpr),
            HttpStatus.OK,
        )
    }

    @GetMapping("/")
    fun getSykmeldingerByUserIdent(
        @RequestHeader("Ident") ident: String,
        @RequestHeader("Orgnr") orgnr: String
    ): ResponseEntity<Any> {
        return ResponseEntity(
            sykmeldingService.getSykmeldingerByIdent(ident, orgnr),
            HttpStatus.OK,
        )
    }

    @GetMapping("/{sykmeldingId}/pdf", produces = ["application/pdf"])
    fun getSykmeldingPdf(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String
    ): ResponseEntity<Any> {
        return ResponseEntity(
            sykmeldingPdfService.getPdf(sykmeldingId.toString(), hpr),
            HttpStatus.OK,
        )
    }

}
