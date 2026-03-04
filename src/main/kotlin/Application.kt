package no.nav.tsm

import io.ktor.server.application.*
import modules.behandler.configureBehandlerModule
import no.nav.tsm.modules.kafka.configureKafkaModule
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingerModule
import no.nav.tsm.plugins.configureDatabase
import no.nav.tsm.plugins.configureDependencies
import no.nav.tsm.plugins.configureMonitoring
import no.nav.tsm.plugins.configureOpenAPI

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Global configuration
    configureDependencies()
    configureMonitoring()
    configureOpenAPI()
    configureDatabase()

    // Specific modules
    configureSykmeldingerModule()
    configureBehandlerModule()
    configureKafkaModule()
}
