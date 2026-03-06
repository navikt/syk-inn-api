package no.nav.tsm

import io.ktor.server.application.*
import modules.behandler.configureBehandlerModule
import modules.jobs.configureJobsModule
import modules.kafka.configureKafkaModule
import modules.sykmelder.configureSykmelderModule
import modules.sykmeldinger.configureSykmeldingerModule
import plugins.*
import plugins.auth.configureAuthentication

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
    configureAuthentication()

    // Specific modules
    configureSykmeldingerModule()
    configureSykmelderModule()
    configureBehandlerModule()
    configureKafkaModule()
    configureJobsModule()

    // TODO: Temporary
    configureTestStuff()
}
