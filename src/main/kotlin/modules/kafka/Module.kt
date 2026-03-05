package modules.kafka

import io.ktor.server.application.Application
import modules.kafka.consume.configureSykmeldingerKafkaConsumerDependencies

fun Application.configureKafkaModule() {
    configureSykmeldingerKafkaConsumerDependencies()
}
