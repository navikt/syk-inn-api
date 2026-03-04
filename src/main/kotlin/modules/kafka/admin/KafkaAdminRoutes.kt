package no.nav.tsm.modules.kafka.admin

import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import io.ktor.server.routing.routing
import no.nav.tsm.core.logger
import no.nav.tsm.modules.kafka.consume.SykmeldingConsumerJobManager

fun Application.configureKafkaAdminRoutes() {
    val logger = logger()
    val service: SykmeldingConsumerJobManager by dependencies

    routing {
        // TODO: Obo auth token, verifiser team medlem (group?)

        route("/internal/kafka/consumer") {
            post("/start") {
                logger.info("Got admin command to START consumer from USER (TODO)")

                service.start()
            }
            post("/stop") {
                logger.info("Got admin command to STOP consumer from USER (TODO)")

                service.stop()
            }
        }
    }
}