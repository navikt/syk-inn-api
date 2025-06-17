package no.nav.tsm.syk_inn_api.sykmelding

import java.util.*
import no.nav.tsm.syk_inn_api.sykmelding.CreateSykmelding.toResponseEntity
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.secureLogger
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
    private val logger = logger()
    private val securelog = secureLogger()

    @PostMapping
    fun createSykmelding(@RequestBody payload: OpprettSykmeldingPayload): ResponseEntity<Any> {
        securelog.info("Received request to create sykmelding with payload: $payload")
        if (payload.meta.pasientIdent.isBlank() || payload.meta.sykmelderHpr.isBlank()) {
            return ResponseEntity.badRequest().body("Pasient fnr and sykmelder hpr are required")
        }

        val sykmeldingResult = sykmeldingService.createSykmelding(payload)

        return sykmeldingResult.fold(
            { error -> error.toResponseEntity() },
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
    ): ResponseEntity<Any> {
        val sykmelding = sykmeldingService.getSykmeldingById(sykmeldingId, hpr)

        return if (sykmelding == null) {
            ResponseEntity.notFound().build()
        } else {
            ResponseEntity.ok(sykmelding)
        }
    }

    @GetMapping
    fun getSykmeldingerByUserIdent(
        @RequestHeader("Ident") ident: String,
        @RequestHeader("HPR") hpr: String,
    ): ResponseEntity<Any> =
        sykmeldingService.getSykmeldingerByIdent(ident, hpr).fold(
            { ResponseEntity.ok(it) },
        ) {
            ResponseEntity.internalServerError().build()
        }
}

object CreateSykmelding {
    /**
     * Rule outcomes that are NotOk are does not error the response completely, but rather return a
     * well formatted reason to the frontend.
     */
    data class RuleOutcome(
        val status: String,
        val message: String,
        val rule: String,
        val tree: String
    )

    /**
     * Handles all error cases for SykmeldingCreationErrors, and maps them to appropriate HTTP
     * responses.
     */
    fun SykmeldingService.SykmeldingCreationErrors.toResponseEntity(): ResponseEntity<Any> =
        when (this) {
            is SykmeldingService.SykmeldingCreationErrors.RuleValidation -> {
                ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(
                        RuleOutcome(
                            status = this.result.outcome.status.name,
                            message = this.result.outcome.reason.sykmelder,
                            rule = this.result.outcome.rule,
                            tree = this.result.outcome.tree,
                        ),
                    )
            }
            is SykmeldingService.SykmeldingCreationErrors.PersistenceError ->
                ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Failed to persist sykmelding")
            is SykmeldingService.SykmeldingCreationErrors.ResourceError ->
                ResponseEntity.status(
                        HttpStatus.INTERNAL_SERVER_ERROR,
                    )
                    .body("Failed to fetch required resources")
        }
}
