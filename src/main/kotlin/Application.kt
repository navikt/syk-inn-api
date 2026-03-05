package no.nav.tsm

import io.ktor.server.application.*
import modules.behandler.configureBehandlerModule
import modules.jobs.configureJobsModule
import modules.kafka.configureKafkaModule
import modules.sykmelder.configureSykmelderModule
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
    configureSykmelderModule()
    configureSykmeldingerModule()
    configureBehandlerModule()
    configureKafkaModule()
    configureJobsModule()

    // TODO: Temporary
    configureTestStuff()
}
