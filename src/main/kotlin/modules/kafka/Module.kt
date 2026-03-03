package no.nav.tsm.modules.kafka

import io.ktor.server.application.Application

fun Application.configureKafka() {

    configureKafkaDependencies()
    configureKafkaAdminRoutes()
}
