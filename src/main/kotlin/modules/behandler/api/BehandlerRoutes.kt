package modules.behandler.api

import io.ktor.http.HttpStatusCode
import io.ktor.openapi.jsonSchema
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.openapi.describe
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import io.ktor.utils.io.ExperimentalKtorApi
import modules.behandler.api.payloads.OpprettSykmelding
import no.nav.tsm.modules.sykmeldinger.SykmeldingService

@OptIn(ExperimentalKtorApi::class)
fun Application.configureBehandlerRoutes() {
    val sir: SykmeldingService by dependencies

    routing {
        /** TODO Only test endpoints */
        get("/test") {
            val sykmeldinger = sir.test()

            call.respond(HttpStatusCode.Created, sykmeldinger)
        }
        post("/create-boio") {
            val newSykmelding = sir.createBoio()

            call.respond(HttpStatusCode.Created, newSykmelding)
        }

        route("/api/sykmelding") {
            post { TODO("Stub for create sykmelding") }
                .describe {
                    summary = "Create a new sykmelding"
                    description =
                        "Will execute rules based on logged in users HPR authorizations and other metadata."
                    requestBody { schema = jsonSchema<OpprettSykmelding.Payload>() }
                    responses {
                        HttpStatusCode.Created { description = "The newly created sykmelding" }
                    }
                }
            post("/verify") { TODO("Stub for verify sykmelding") }
                .describe {
                    summary = "Verifying the contents of a sykmelding"
                    description =
                        "Verify what the rule execution will return, without actually creating the sykmelding"
                    requestBody { schema = jsonSchema<OpprettSykmelding.Payload>() }
                    responses {
                        HttpStatusCode.OK {
                            description =
                                "The result of the rule execution, without creating the sykmelding"
                        }
                    }
                }
            get("/{id}") {
                    val id = call.parameters["id"] ?: return@get call.respond("Missing id")

                    TODO("Stub for get sykmelding by id: $id")
                }
                .describe {
                    summary = "Get a sykmelding by id"
                    description =
                        "Will return the sykmelding with the given id, if it exists and the logged in user has access to it."
                    responses {
                        HttpStatusCode.OK { description = "The sykmelding with the given id" }
                        HttpStatusCode.NotFound {
                            description =
                                "No sykmelding with the given id was found, or the logged in user does not have access to it."
                        }
                    }
                }
            get { TODO("Stub for get all sykmeldinger") }
                .describe {
                    summary = "Get all sykmeldinger for the logged in user"
                    description =
                        "Will return all sykmeldinger that the logged in user has access to. Sykmeldinger from other practitioners will be a special 'redacted' variant."
                    responses {
                        HttpStatusCode.OK {
                            description =
                                "A list of sykmeldinger that the logged in user has access to"
                        }
                    }
                }
        }
    }
}
