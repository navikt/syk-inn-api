package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.modules.kafka.configureKafka
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingerApi
import no.nav.tsm.plugins.configureDatabase
import no.nav.tsm.plugins.configureDependencies
import no.nav.tsm.plugins.configureMonitoring
import no.nav.tsm.plugins.configureOpenAPI
import no.nav.tsm.plugins.configureSerialization

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Global configuration
    configureDependencies()
    configureSerialization()
    configureMonitoring()
    configureOpenAPI()
    configureDatabase()

    // Specific modules
    configureSykmeldingerApi()
    configureKafka()
}
