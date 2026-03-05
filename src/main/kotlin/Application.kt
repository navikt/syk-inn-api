package no.nav.tsm

import io.ktor.server.application.*
import modules.behandler.configureBehandlerModule
import modules.external.configureExternalModule
import modules.kafka.configureKafkaModule
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingerModule
import plugins.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

fun Application.module() {
    // Global configuration
    configureDependencies()
    configureMonitoring()
    configureOpenAPI()
    configureDatabase()
    configureSerialization()

    // Specific modules
    configureExternalModule()
    configureSykmeldingerModule()
    configureBehandlerModule()
    configureKafkaModule()

    // TODO: Temporary
    configureTestStuff()
}
