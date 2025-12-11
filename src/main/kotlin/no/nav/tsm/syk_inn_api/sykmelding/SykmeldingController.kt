package no.nav.tsm.syk_inn_api.sykmelding

import java.util.*
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.sykmelding.CreateSykmelding.RuleOutcome
import no.nav.tsm.syk_inn_api.sykmelding.CreateSykmelding.toResponseEntity
import no.nav.tsm.syk_inn_api.sykmelding.metrics.SykmeldingMetrics
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingResponse
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
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
    private val sykmeldingMetrics: SykmeldingMetrics,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    @PostMapping
    fun createSykmelding(@RequestBody payload: OpprettSykmeldingPayload): ResponseEntity<Any> {
        teamLogger.info("Received request to create sykmelding with payload: $payload")

        if (payload.meta.pasientIdent.isBlank() || payload.meta.sykmelderHpr.isBlank()) {
            return ResponseEntity.badRequest().body("Pasient fnr and sykmelder hpr are required")
        }

        val sykmeldingResult = sykmeldingService.createSykmelding(payload)

        return sykmeldingResult.fold(
            { error -> error.toResponseEntity() },
        ) { sykmelding ->
            logger.info("Sykmelding created successfully with ID: ${sykmelding.sykmeldingId}")
            ResponseEntity.status(HttpStatus.CREATED).body(sykmelding)
        }
    }

    @PostMapping("/verify")
    fun verifySykmelding(@RequestBody payload: OpprettSykmeldingPayload): ResponseEntity<Any> {
        teamLogger.info("Received request to verify sykmelding with payload: $payload")

        val verificationResult = sykmeldingService.verifySykmelding(payload)

        return verificationResult.fold(
            { error -> error.toResponseEntity() },
        ) { verification ->
            ResponseEntity.status(HttpStatus.OK)
                .body(
                    when (verification) {
                        is RegulaResult.NotOk ->
                            RuleOutcome(
                                status = verification.outcome.status,
                                message = verification.outcome.reason.sykmelder,
                                rule = verification.outcome.rule,
                                tree = verification.outcome.tree,
                            )
                        is RegulaResult.Ok -> true
                    },
                )
        }
    }

    @GetMapping("/{sykmeldingId}")
    fun getSykmeldingById(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String,
    ): ResponseEntity<SykmeldingResponse> {
        val sykmelding = sykmeldingService.getSykmeldingById(sykmeldingId)
            ?: return ResponseEntity.notFound().build()

        val sykmeldingAccessControlled = sykmeldingAccessControl(hpr, sykmelding)
        if (sykmeldingAccessControlled == null) {
            sykmeldingMetrics.incrementAccessControlDenied("/api/sykmelding/{id}")
            return ResponseEntity.notFound().build()
        }

        return ResponseEntity.ok(sykmeldingAccessControlled)
    }

    @GetMapping
    fun getSykmeldingerByUserIdent(
        @RequestHeader("Ident") ident: String,
        @RequestHeader("HPR") hpr: String,
    ): ResponseEntity<List<SykmeldingResponse>> {
        return sykmeldingService.getSykmeldingerByIdent(ident).fold(
            { sykmeldingList ->
                val accessControlledList: List<SykmeldingResponse> =
                    sykmeldingList.mapNotNull { sykmeldingAccessControl(hpr, it) }
                ResponseEntity.ok(accessControlledList)
            },
        ) {
            ResponseEntity.internalServerError().build()
        }
    }
}

object CreateSykmelding {
    /**
     * For rule outcomes that are NotOk we present a well formatted reason to the frontend together
     * with the status-code 422 Unprocessable Entity
     */
    data class RuleOutcome(
        val status: RegulaOutcomeStatus,
        val message: String,
        val rule: String,
        val tree: String
    )

    data class ErrorMessage(
        val message: String,
    )

    /**
     * Handles all error cases for SykmeldingCreationErrors, and maps them to appropriate HTTP
     * responses.
     */
    fun SykmeldingService.SykmeldingCreationErrors.toResponseEntity(): ResponseEntity<Any> =
        when (this) {
            is SykmeldingService.SykmeldingCreationErrors.PersonDoesNotExist ->
                ResponseEntity.status(
                        HttpStatus.UNPROCESSABLE_ENTITY,
                    )
                    .body(ErrorMessage("Person does not exist"))
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
