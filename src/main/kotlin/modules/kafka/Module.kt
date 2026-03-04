package no.nav.tsm.modules.kafka

import io.ktor.server.application.Application
import no.nav.tsm.modules.kafka.admin.configureKafkaAdminRoutes
import no.nav.tsm.modules.kafka.consume.configureKafkaConsumerJob
import no.nav.tsm.modules.kafka.consume.configureSykmeldingerKafkaConsumerDependencies

fun Application.configureKafka() {
    configureSykmeldingerKafkaConsumerDependencies()
    configureKafkaAdminRoutes()
    configureKafkaConsumerJob()
}
