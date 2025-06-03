package no.nav.tsm.syk_inn_api.sykmelding

import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
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
) {

    private val logger = LoggerFactory.getLogger(this::class.java)

    @PostMapping
    fun createSykmelding(@RequestBody payload: SykmeldingPayload): ResponseEntity<Any> {
        logger.info("Received request to create sykmelding with payload: $payload")
        if (payload.pasientFnr.isBlank() || payload.sykmelderHpr.isBlank()) {
            return ResponseEntity.badRequest().body("Pasient fnr and sykmelder hpr are required")
        }

        val sykmeldingResult = sykmeldingService.createSykmelding(payload)

        return sykmeldingResult.fold(
            { it.toResponseEntity() },
        ) { sykmelding ->
            logger.info(
                "Sykmelding created successfully with ID: ${sykmelding.sykmeldingId}",
            )

            ResponseEntity.status(HttpStatus.CREATED).body(sykmelding)
        }
    }

    @GetMapping("/{sykmeldingId}")
    fun getSykmeldingById(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String,
    ): ResponseEntity<Any> =
        sykmeldingService.getSykmeldingById(sykmeldingId, hpr).fold(
            { ResponseEntity.ok(it) },
        ) {
            when (it) {
                is IllegalArgumentException -> ResponseEntity.notFound().build()
                else -> ResponseEntity.internalServerError().build()
            }
        }

    @GetMapping("/")
    fun getSykmeldingerByUserIdent(
        @RequestHeader("Ident") ident: String,
        @RequestHeader("Orgnr") orgnr: String
    ): ResponseEntity<Any> =
        sykmeldingService.getSykmeldingerByIdent(ident, orgnr).fold(
            { ResponseEntity.ok(it) },
        ) {
            ResponseEntity.internalServerError().build()
        }
}

/**
 * Handles all error cases for SykmeldingCreationErrors, and maps them to appropriate HTTP
 * responses.
 */
private fun SykmeldingService.SykmeldingCreationErrors.toResponseEntity(): ResponseEntity<Any> =
    when (this) {
        SykmeldingService.SykmeldingCreationErrors.RULE_VALIDATION ->
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Rule validation failed")
        SykmeldingService.SykmeldingCreationErrors.PERSISTENCE_ERROR ->
            ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Failed to persist sykmelding")
        SykmeldingService.SykmeldingCreationErrors.RESOURCE_ERROR ->
            ResponseEntity.status(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                )
                .body("Failed to fetch required resources")
    }
