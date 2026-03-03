package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.modules.kafka.configureKafka
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingerApi
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
    configureSykmeldingerApi()
    configureKafka()
}
