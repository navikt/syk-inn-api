package modules.kafka.consume

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*

fun Application.configureSykmeldingerKafkaConsumerDependencies() {
    dependencies {
        provide<SykmeldingConsumer>(SykmeldingConsumer::class)
        provide<SykmeldingConsumerService>(SykmeldingConsumerService::class)
        provide<SykmeldingConsumerJobManager>(SykmeldingConsumerJobManager::class)
    }
}
