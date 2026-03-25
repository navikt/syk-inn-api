package no.nav.tsm.modules.behandler

import arrow.core.getOrElse
import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.util.logging.error
import io.ktor.utils.io.ExperimentalKtorApi
import java.util.UUID
import no.nav.tsm.core.logger
import no.nav.tsm.modules.behandler.access.BehandlerAccessControlService
import no.nav.tsm.modules.behandler.mappers.toBehandlerSykmeldingVerify
import no.nav.tsm.modules.behandler.mappers.toSykInnSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingRedacted
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingVerify
import no.nav.tsm.modules.behandler.payloads.OpprettSykmelding
import no.nav.tsm.modules.sykmeldinger.SykmeldingerService
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.plugins.auth.MACHINE_TOKEN_AUTH

@OptIn(ExperimentalKtorApi::class)
fun Application.configureBehandlerRoutes() {
    val logger = logger()
    val accessControlService: BehandlerAccessControlService by dependencies
    val sykmeldingerService: SykmeldingerService by dependencies

    routing {
        authenticate(MACHINE_TOKEN_AUTH) {
            route("/api/sykmelding") {
                post {
                        try {
                            val payload: OpprettSykmelding.Payload = call.receive()
                            val unruledSykmelding = payload.toSykInnSykmelding()
                            val createdSykmelding =
                                sykmeldingerService.create(unruledSykmelding).getOrElse {
                                    return@post when (it) {
                                        SykmeldingerService.CreateErrors.PersonNotInPdl ->
                                            call.respond<GenericError>(
                                                HttpStatusCode.UnprocessableEntity,
                                                GenericError("Person does not exist"),
                                            )

                                        SykmeldingerService.CreateErrors.UnknownResourceError,
                                        SykmeldingerService.CreateErrors.RuleError ->
                                            call.respond<GenericError>(
                                                HttpStatusCode.InternalServerError,
                                                GenericError("Internal server error"),
                                            )
                                    }
                                }

                            val accessControlledSykmelding =
                                accessControlService.toRedactedIfNeeded(
                                    sykInnSykmelding = createdSykmelding,
                                    currentBehandlerHpr = createdSykmelding.meta.hpr,
                                )

                            when (accessControlledSykmelding) {
                                null -> {
                                    logger.error("Freshly created sykmelding cannot be null")
                                    call.respond<GenericError>(
                                        HttpStatusCode.InternalServerError,
                                        GenericError("Internal server error"),
                                    )
                                }

                                is BehandlerSykmeldingRedacted -> {
                                    logger.error("Freshly created sykmelding cannot be redacted")
                                    call.respond<GenericError>(
                                        HttpStatusCode.InternalServerError,
                                        GenericError("Internal server error"),
                                    )
                                }

                                is BehandlerSykmeldingFull -> {
                                    call.respond<BehandlerSykmeldingFull>(
                                        HttpStatusCode.Created,
                                        accessControlledSykmelding,
                                    )
                                }
                            }
                        } catch (ex: Exception) {
                            logger.error(ex)
                            call.respond(
                                HttpStatusCode.InternalServerError,
                                GenericError("Unable to create sykmelding"),
                            )
                        }
                    }
                    .describe {
                        summary = "Create a new sykmelding"
                        description =
                            "Will execute rules based on logged in users HPR authorizations and other metadata."
                        requestBody { schema = jsonSchema<OpprettSykmelding.Payload>() }
                        responses {
                            HttpStatusCode.Created {
                                description = "The newly created sykmelding"
                                schema = jsonSchema<BehandlerSykmeldingFull>()
                            }
                            HttpStatusCode.UnprocessableEntity {
                                description =
                                    "The person does not exist in PDL and therefore can't be verified"
                                schema = jsonSchema<GenericError>()
                            }
                        }
                    }
                post("/verify") {
                        val payload: OpprettSykmelding.Payload = call.receive()
                        val sykmelding: UnverifiedSykInnSykmelding = payload.toSykInnSykmelding()
                        val rule: SykInnSykmeldingRuleResult =
                            sykmeldingerService.verify(sykmelding).getOrElse {
                                return@post when (it) {
                                    SykmeldingerService.CreateErrors.PersonNotInPdl ->
                                        call.respond<GenericError>(
                                            HttpStatusCode.UnprocessableEntity,
                                            GenericError("Person does not exist"),
                                        )

                                    SykmeldingerService.CreateErrors.RuleError,
                                    SykmeldingerService.CreateErrors.UnknownResourceError ->
                                        call.respond<GenericError>(
                                            HttpStatusCode.InternalServerError,
                                            GenericError("Internal server error"),
                                        )
                                }
                            }

                        when (rule) {
                            is SykInnSykmeldingRuleResult.OK ->
                                call.respond<Boolean>(HttpStatusCode.OK, true)

                            is SykInnSykmeldingRuleResult.Outcome ->
                                call.respond<BehandlerSykmeldingVerify>(
                                    HttpStatusCode.OK,
                                    rule.toBehandlerSykmeldingVerify(),
                                )
                        }
                    }
                    .describe {
                        summary = "Verifying the contents of a sykmelding"
                        description =
                            "Verify what the rule execution will return, without actually creating the sykmelding"
                        requestBody { schema = jsonSchema<OpprettSykmelding.Payload>() }
                        responses {
                            HttpStatusCode.OK {
                                description =
                                    "The result of the rule execution, without creating the sykmelding"
                                // TODO: How to represent union between boolean and
                                // BehandlerSykmeldingVerify?
                                schema = jsonSchema<BehandlerSykmeldingVerify>()
                            }
                            HttpStatusCode.UnprocessableEntity {
                                description =
                                    "The person does not exist in PDL and therefore can't be verified"
                                schema = jsonSchema<GenericError>()
                            }
                        }
                    }
                get("/{id}") {
                        val id: UUID =
                            call.parameters["id"]?.let { UUID.fromString(it) }
                                ?: return@get call.respond(
                                    HttpStatusCode.BadRequest,
                                    GenericError("Invalid ID"),
                                )

                        val sykmelding: VerifiedSykInnSykmelding =
                            sykmeldingerService.byId(id).getOrElse {
                                return@get when (it) {
                                    SykmeldingerService.GetErrors.NotFound ->
                                        call.respond<GenericError>(
                                            HttpStatusCode.NotFound,
                                            GenericError("Unable to find sykmelding"),
                                        )

                                    SykmeldingerService.GetErrors.UnknownError ->
                                        call.respond<GenericError>(
                                            HttpStatusCode.InternalServerError,
                                            GenericError("Internal server error"),
                                        )
                                }
                            }

                        val accessControlledSykmelding =
                            accessControlService.toRedactedIfNeeded(sykmelding, "TODO")
                                // Don't reveal anything, pretend its a simple 404
                                ?: return@get call.respond<GenericError>(
                                    HttpStatusCode.NotFound,
                                    GenericError("Unable to find sykmelding"),
                                )

                        call.respond<BehandlerSykmelding>(
                            HttpStatusCode.Created,
                            accessControlledSykmelding,
                        )
                    }
                    .describe {
                        summary = "Get a sykmelding by id"
                        description =
                            "Will return the sykmelding with the given id, if it exists and the logged in user has access to it."
                        responses {
                            HttpStatusCode.OK {
                                description = "The sykmelding with the given id"
                                schema = jsonSchema<BehandlerSykmelding>()
                            }
                            HttpStatusCode.NotFound {
                                description =
                                    "No sykmelding with the given id was found, or the logged in user does not have access to it."
                                schema = jsonSchema<GenericError>()
                            }
                            HttpStatusCode.InternalServerError {
                                schema = jsonSchema<GenericError>()
                            }
                            HttpStatusCode.BadRequest { schema = jsonSchema<GenericError>() }
                        }
                    }
                get {
                        val hprHeader =
                            requireNotNull(call.request.headers["HPR"]) { "HPR header is missing" }

                        val identHeader =
                            requireNotNull(call.request.headers["Ident"]) { "Missing Ident header" }
                        val allSykmeldinger: List<VerifiedSykInnSykmelding> =
                            sykmeldingerService.byIdent(identHeader).getOrElse {
                                return@get call.respond<GenericError>(
                                    HttpStatusCode.InternalServerError,
                                    GenericError("Internal server error"),
                                )
                            }
                        val accessControlledSykmeldinger: List<BehandlerSykmelding> =
                            allSykmeldinger.mapNotNull {
                                accessControlService.toRedactedIfNeeded(
                                    it,
                                    currentBehandlerHpr = hprHeader,
                                )
                            }

                        call.respond<List<BehandlerSykmelding>>(
                            HttpStatusCode.OK,
                            accessControlledSykmeldinger,
                        )
                    }
                    .describe {
                        summary = "Get all sykmeldinger for the logged in user"
                        description =
                            "Will return all sykmeldinger that the logged in user has access to. Sykmeldinger from other practitioners will be a special 'redacted' variant."
                        responses {
                            HttpStatusCode.OK {
                                description =
                                    "A list of sykmeldinger that the logged in user has access to"
                                schema = jsonSchema<List<BehandlerSykmelding>>()
                            }
                            HttpStatusCode.InternalServerError {
                                schema = jsonSchema<GenericError>()
                            }
                        }
                    }
            }
        }
    }
}

private data class GenericError(val message: String)
