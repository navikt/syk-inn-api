package modules.external

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.external.clients.texas.TexasClient

fun Application.configureExternalDependencies() {
    dependencies {
        key<HttpClient>("ExternalHttpClient") { createExternalApiHttpClient() }
        provide(TexasClient::class)
    }
}

private fun createExternalApiHttpClient() {
    HttpClient { install(ContentNegotiation) { jackson() } }
}
