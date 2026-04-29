@file:OptIn(ExperimentalKtorApi::class)

package no.nav.tsm.modules.behandler

import arrow.core.Either
import arrow.core.raise.catch
import arrow.core.raise.either
import arrow.core.raise.ensure
import arrow.core.raise.ensureNotNull
import io.ktor.http.*
import io.ktor.openapi.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.plugins.*
import io.ktor.server.plugins.di.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.routing.openapi.*
import io.ktor.utils.io.ExperimentalKtorApi
import java.util.*
import no.nav.tsm.core.logger
import no.nav.tsm.core.otel.failSpan
import no.nav.tsm.modules.behandler.access.BehandlerAccessControlService
import no.nav.tsm.modules.behandler.mappers.toBehandlerSykmeldingVerify
import no.nav.tsm.modules.behandler.mappers.toSykInnSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerOpprettSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingVerify
import no.nav.tsm.modules.sykmeldinger.SykmeldingerService
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.plugins.auth.MACHINE_TOKEN_AUTH

private val logger = logger()

fun Application.configureBehandlerRoutes() {
    val accessControlService: BehandlerAccessControlService by dependencies
    val sykmeldingerService: SykmeldingerService by dependencies

    routing {
        authenticate(MACHINE_TOKEN_AUTH) {
            route("/api/sykmelding") {
                post {
                        either<GenericHttpError, BehandlerSykmeldingFull> {
                                val payload = call.receiveOpprettPayload().bind()
                                val sykmelding = payload.toSykInnSykmelding()
                                val hpr = sykmelding.meta.behandlerHpr
                                val created: VerifiedSykInnSykmelding =
                                    sykmeldingerService
                                        .create(sykmelding)
                                        .mapLeft { it.createdToHttpError() }
                                        .bind()

                                val result =
                                    accessControlService.toRedactedIfNeeded(
                                        sykInnSykmelding = created,
                                        currentBehandlerHpr = hpr,
                                    )

                                ensureNotNull(result) { InternalServerError }
                                ensure(result is BehandlerSykmeldingFull) { InternalServerError }

                                result
                            }
                            .fold(
                                { call.respond(it.code, MessageBody(it.message)) },
                                { call.respond(HttpStatusCode.OK, it) },
                            )
                    }
                    .describe {
                        summary = "Create a new sykmelding"
                        description =
                            "Will execute rules based on logged in users HPR authorizations and other metadata."
                        requestBody { schema = jsonSchema<BehandlerOpprettSykmelding.Payload>() }
                        responses {
                            HttpStatusCode.Created {
                                description = "The newly created sykmelding"
                                schema = jsonSchema<BehandlerSykmeldingFull>()
                            }
                            HttpStatusCode.UnprocessableEntity {
                                description =
                                    "The person does not exist in PDL and therefore can't be verified"
                                schema = jsonSchema<MessageBody>()
                            }
                        }
                    }
                post("/verify") {
                        either {
                                val payload = call.receiveOpprettPayload().bind()
                                val sykmelding = payload.toSykInnSykmelding()

                                sykmeldingerService
                                    .verify(sykmelding)
                                    .mapLeft { it.createdToHttpError() }
                                    .bind()
                            }
                            .fold(
                                { error: GenericHttpError ->
                                    call.respond(error.code, MessageBody(error.message))
                                },
                                { rule: SykInnSykmeldingRuleResult ->
                                    call.respond<BehandlerSykmeldingVerify>(
                                        HttpStatusCode.OK,
                                        rule.toBehandlerSykmeldingVerify(),
                                    )
                                },
                            )
                    }
                    .describe {
                        summary = "Verifying the contents of a sykmelding"
                        description =
                            "Verify what the rule execution will return, without actually creating the sykmelding"
                        requestBody { schema = jsonSchema<BehandlerOpprettSykmelding.Payload>() }
                        responses {
                            HttpStatusCode.OK {
                                description =
                                    "The result of the rule execution, without creating the sykmelding"
                                schema = jsonSchema<BehandlerSykmeldingVerify>()
                            }
                            HttpStatusCode.UnprocessableEntity {
                                description =
                                    "The person does not exist in PDL and therefore can't be verified"
                                schema = jsonSchema<MessageBody>()
                            }
                        }
                    }
                get("/{id}") {
                        either<GenericHttpError, BehandlerSykmelding> {
                                val id: UUID = call.uuidPathParam("id").bind()
                                val hpr = call.hprHeader().bind()

                                val sykmelding: VerifiedSykInnSykmelding =
                                    sykmeldingerService
                                        .byId(id)
                                        .mapLeft { it.getToHttpError() }
                                        .bind()

                                val accessControlledSykmelding =
                                    accessControlService.toRedactedIfNeeded(sykmelding, hpr)

                                ensureNotNull(accessControlledSykmelding) {
                                    logger.warn(
                                        "User $hpr tried to access someone else's non-OK sykmelding with id $id"
                                    )

                                    SykmeldingNotFound
                                }

                                accessControlledSykmelding
                            }
                            .fold(
                                { error: GenericHttpError ->
                                    call.respond(error.code, MessageBody(error.message))
                                },
                                { call.respond(HttpStatusCode.OK, it) },
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
                                schema = jsonSchema<MessageBody>()
                            }
                            HttpStatusCode.InternalServerError {
                                schema = jsonSchema<MessageBody>()
                            }
                            HttpStatusCode.BadRequest { schema = jsonSchema<MessageBody>() }
                        }
                    }
                get {
                        either<GenericHttpError, List<BehandlerSykmelding>> {
                                val hprHeader = call.hprHeader().bind()
                                val identHeader = call.identHeader().bind()

                                val allSykmeldinger: List<VerifiedSykInnSykmelding> =
                                    sykmeldingerService
                                        .byIdent(identHeader)
                                        .mapLeft { it.getToHttpError() }
                                        .bind()

                                val accessControlledSykmeldinger: List<BehandlerSykmelding> =
                                    allSykmeldinger.mapNotNull {
                                        accessControlService.toRedactedIfNeeded(
                                            it,
                                            currentBehandlerHpr = hprHeader,
                                        )
                                    }

                                accessControlledSykmeldinger
                            }
                            .fold(
                                { call.respond(it.code, MessageBody(it.message)) },
                                { call.respond(HttpStatusCode.OK, it) },
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
                                schema = jsonSchema<MessageBody>()
                            }
                        }
                    }
            }
        }
    }
}

private fun SykmeldingerService.CreateErrors.createdToHttpError(): GenericHttpError =
    when (this) {
        SykmeldingerService.CreateErrors.PersonNotInPdl ->
            GenericHttpError(HttpStatusCode.UnprocessableEntity, "Person does not exist")

        SykmeldingerService.CreateErrors.UnknownResourceError,
        SykmeldingerService.CreateErrors.RuleError ->
            GenericHttpError(HttpStatusCode.InternalServerError, "Internal server error")
    }

private fun SykmeldingerService.GetErrors.getToHttpError(): GenericHttpError =
    when (this) {
        SykmeldingerService.GetErrors.NotFound -> SykmeldingNotFound
        SykmeldingerService.GetErrors.UnknownError -> InternalServerError
    }

private suspend fun RoutingCall.receiveOpprettPayload():
    Either<GenericHttpError, BehandlerOpprettSykmelding.Payload> = either {
    catch(
        { receive<BehandlerOpprettSykmelding.Payload>() },
        { e: BadRequestException ->
            logger.error("OpprettSykmelding.Payload body parsing failed", e.failSpan())

            raise(GenericHttpError(HttpStatusCode.BadRequest, "Unable to parse body"))
        },
    )
}

private fun RoutingCall.hprHeader(): Either<GenericHttpError, String> = either {
    ensureNotNull(request.headers["HPR"]) {
        GenericHttpError(HttpStatusCode.BadRequest, "HPR header is missing")
    }
}

private fun RoutingCall.identHeader(): Either<GenericHttpError, String> = either {
    ensureNotNull(request.headers["Ident"]) {
        GenericHttpError(HttpStatusCode.BadRequest, "HPR header is missing")
    }
}

/**
 * Path params should always be found if the path and name matches. Any error that occurs here is
 * purely a developer mistake.
 */
private fun RoutingCall.uuidPathParam(name: String): Either<GenericHttpError, UUID> = either {
    ensureNotNull(parameters[name]) {
            logger.error("Invalid path parameter: $name, path is ${request.path()}")

            InternalServerError
        }
        .let { UUID.fromString(it) }
}

/**
 * The actual payload of any non-2xx responses. Used for OpenAPI generation and call.respond(...).
 */
private data class MessageBody(val message: String)

/** Normalized representation of HttpErrors, used in Either<GenericHttpError, ...> */
private data class GenericHttpError(val code: HttpStatusCode, val message: String)

private val InternalServerError =
    GenericHttpError(HttpStatusCode.InternalServerError, "Internal server error")

private val SykmeldingNotFound =
    GenericHttpError(HttpStatusCode.NotFound, "Unable to find sykmelding")
