package no.nav.tsm.modules.kafka

import io.ktor.server.application.Application
import no.nav.tsm.modules.kafka.consume.configureSykmeldingerKafkaConsumerDependencies

fun Application.configureKafkaModule() {
    configureSykmeldingerKafkaConsumerDependencies()
}
