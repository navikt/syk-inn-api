package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.modules.admin.configureJobsModule
import no.nav.tsm.modules.behandler.configureBehandlerModule
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingerModule
import no.nav.tsm.modules.sykmeldinger.jobs.configureKafkaModule
import no.nav.tsm.plugins.*
import no.nav.tsm.plugins.auth.configureAuthentication

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
    configureBehandlerModule()
    configureKafkaModule()
    configureJobsModule()
}
