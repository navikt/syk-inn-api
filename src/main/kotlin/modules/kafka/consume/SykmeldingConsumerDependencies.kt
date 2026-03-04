package no.nav.tsm.modules.kafka.consume

import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationStarted
import io.ktor.server.application.ApplicationStopping
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.plugins.di.resolve
import kotlinx.coroutines.launch
import no.nav.tsm.core.logger


fun Application.configureSykmeldingerKafkaConsumerDependencies() {
    dependencies {
        provide<SykmeldingConsumerService> {
            SykmeldingConsumerService(initializeSykmeldingerConsumer(resolve()))
        }
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