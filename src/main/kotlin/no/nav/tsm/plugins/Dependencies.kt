package no.nav.tsm.plugins

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.*
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import no.nav.tsm.core.Environment
import no.nav.tsm.core.initializeEnvironment

fun Application.configureDependencies() {
    val config = environment.config

    dependencies {
        provide<HttpClient> { configureBaseHttpClient() }
        provide<Environment> { initializeEnvironment(config) }
    }
}

private fun configureBaseHttpClient(): HttpClient = HttpClient(Apache5) {}
