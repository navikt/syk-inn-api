package no.nav.tsm.modules.kafka.consume

import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.launch
import modules.kafka.consume.SykmeldingConsumerJobManager
import no.nav.tsm.core.logger

fun Application.configureSykmeldingerKafkaConsumerDependencies() {
    dependencies {
        provide<SykmeldingConsumer>(SykmeldingConsumer::class)
        provide<SykmeldingConsumerService>(SykmeldingConsumerService::class)
        provide<SykmeldingConsumerJobManager>(SykmeldingConsumerJobManager::class)
    }
}

fun Application.configureKafkaConsumerJob() {
    val log = logger()
    val service: SykmeldingConsumerJobManager by dependencies

    monitor.subscribe(ApplicationStarted) {
        log.info("Application started! Lets Kafka!")

        launch { service.start() }
    }

    monitor.subscribe(ApplicationStopping) {
        log.info("Application stopping! Stopping Kafka ...")
        launch { service.stop() }
    }
}
