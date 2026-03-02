package no.nav.tsm

import io.ktor.server.application.*
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingerApi
import no.nav.tsm.plugins.configureDependencies
import no.nav.tsm.plugins.configureOpenAPI
import no.nav.tsm.plugins.configureMonitoring
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

    // Specific modules
    configureSykmeldingerApi()
}

/**
 *
 * fun Application.module() {
 *     // Global Ktor configuration
 *     configureSerialization()
 *     configureSecurity()
 *     configureFrameworks()
 *     configureMonitoring()
 *     configureOpenAPI()
 *     configureDatabases()
 *
 *     // Different application modules
 *     epjFrontendModule()
 *     fhirAuthModule()
 * }
 *
 */