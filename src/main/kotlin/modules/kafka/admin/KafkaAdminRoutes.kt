package no.nav.tsm.modules.kafka.admin

import core.logger
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import kotlin.collections.mapOf
import modules.kafka.consume.SykmeldingConsumerJobManager

fun Application.configureKafkaAdminRoutes() {
    val logger = logger()
    val service: SykmeldingConsumerJobManager by dependencies

    routing {
        // TODO: Obo auth token, verifiser team medlem (group?)

        route("/internal/kafka/consumer") {
            get("/status") {
                call.respond(HttpStatusCode.OK, mapOf("status" to service.status().name))
            }
            post("/start") {
                logger.info("Got admin command to START consumer from USER (TODO)")

                val started = service.start()
                call.respond(
                    if (started) HttpStatusCode.Accepted else HttpStatusCode.Conflict,
                    mapOf("status" to service.status().name),
                )
            }
            post("/stop") {
                logger.info("Got admin command to STOP consumer from USER (TODO)")

                val stopped = service.stop()
                call.respond(
                    if (stopped) HttpStatusCode.Accepted else HttpStatusCode.Conflict,
                    mapOf("status" to service.status().name),
                )
            }
        }
    }
}
