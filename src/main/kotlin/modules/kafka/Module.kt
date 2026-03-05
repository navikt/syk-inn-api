package modules.kafka

import io.ktor.server.application.Application
import modules.kafka.consume.configureKafkaConsumerJob
import modules.kafka.consume.configureSykmeldingerKafkaConsumerDependencies
import no.nav.tsm.modules.kafka.admin.configureKafkaAdminRoutes

fun Application.configureKafkaModule() {
    configureSykmeldingerKafkaConsumerDependencies()
    configureKafkaAdminRoutes()
    configureKafkaConsumerJob()
}
