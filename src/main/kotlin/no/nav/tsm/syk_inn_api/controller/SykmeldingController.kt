package no.nav.tsm.syk_inn_api.controller

import jakarta.servlet.http.HttpServletRequest
import java.util.*
import no.nav.tsm.syk_inn_api.model.SykmeldingResult
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.service.SykmeldingPdfService
import no.nav.tsm.syk_inn_api.service.SykmeldingService
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
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
        logger.info("Received request to create sykmelding with payload: $payload")
        if (payload.pasientFnr.isBlank() || payload.sykmelderHpr.isBlank()) {
            return ResponseEntity.badRequest().body("Pasient fnr and sykmelder hpr are required")
        }

        val sykmeldingResult = sykmeldingService.createSykmelding(payload)
        if (sykmeldingResult is SykmeldingResult.Failure) {
            return ResponseEntity.status(sykmeldingResult.errorCode)
                .body(sykmeldingResult.errorMessage)
        }
        require(sykmeldingResult is SykmeldingResult.Success) {
            "Expected, but was SykmeldingResult.Failure"
        }
        return ResponseEntity.status(sykmeldingResult.statusCode)
            .body(sykmeldingResult.savedSykmelding)
    }

    @GetMapping("/{sykmeldingId}")
    fun getSykmeldingById(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String,
        request: HttpServletRequest,
    ): ResponseEntity<Any> {
        val sykmeldingResult = sykmeldingService.getSykmeldingById(sykmeldingId, hpr)
        if (sykmeldingResult is SykmeldingResult.Failure) {
            return ResponseEntity.status(sykmeldingResult.errorCode)
                .body(sykmeldingResult.errorMessage)
        }
        require(sykmeldingResult is SykmeldingResult.Success) {
            "Expected, but was SykmeldingResult.Failure"
        }
        return ResponseEntity.status(sykmeldingResult.statusCode)
            .body(sykmeldingResult.savedSykmelding)
    }

    @GetMapping("/")
    fun getSykmeldingerByUserIdent(
        @RequestHeader("Ident") ident: String,
        @RequestHeader("Orgnr") orgnr: String
    ): ResponseEntity<Any> {
        val sykmeldingResult = sykmeldingService.getSykmeldingerByIdent(ident, orgnr)
        if (sykmeldingResult is SykmeldingResult.Failure) {
            return ResponseEntity.status(sykmeldingResult.errorCode)
                .body(sykmeldingResult.errorMessage)
        }
        require(sykmeldingResult is SykmeldingResult.Success) {
            "Expected, but was SykmeldingResult.Failure"
        }
        return ResponseEntity.status(sykmeldingResult.statusCode)
            .body(sykmeldingResult.sykmeldinger)
    }

    @GetMapping("/{sykmeldingId}/pdf", produces = ["application/pdf"])
    fun getSykmeldingPdf(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String
    ): ResponseEntity<Any> {

        val sykmeldingResult = sykmeldingPdfService.getPdf(sykmeldingId.toString(), hpr)
        if (sykmeldingResult is SykmeldingResult.Failure) {
            return ResponseEntity.status(sykmeldingResult.errorCode)
                .body(sykmeldingResult.errorMessage)
        }
        require(sykmeldingResult is SykmeldingResult.Success) {
            "Expected, but was SykmeldingResult.Failure"
        }
        return ResponseEntity.status(sykmeldingResult.statusCode).body(sykmeldingResult.pdf)
    }
}
