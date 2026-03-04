package plugins

import io.ktor.client.HttpClient
import io.ktor.client.engine.apache5.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.*
import io.ktor.server.plugins.di.*
import kotlinx.coroutines.CoroutineScope
import no.nav.tsm.core.Environment
import no.nav.tsm.core.initializeEnvironment

fun Application.configureDependencies() {
    val config = environment.config

    dependencies {
        provide<HttpClient> { configureBaseHttpClient() }
        provide<Environment>() { initializeEnvironment(config) }
        provide<CoroutineScope> { this }
    }
}

private fun configureBaseHttpClient(): HttpClient =
    HttpClient(Apache5) { install(ContentNegotiation) { jackson() } }
