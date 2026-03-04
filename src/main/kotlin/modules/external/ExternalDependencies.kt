package modules.external

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.external.clients.pdl.PdlClient
import modules.external.clients.texas.TexasClient

fun Application.configureExternalDependencies() {
    val baseHttpClient: HttpClient by dependencies

    dependencies {
        provide("RetryHttpClient") { createExternalApiHttpClient(baseHttpClient) }
        provide(TexasClient::class)
        provide(PdlClient::class)
    }
}

private fun createExternalApiHttpClient(baseHttpClient: HttpClient) = HttpClient {
    install(ContentNegotiation) { jackson() }
    install(HttpRequestRetry) {
        retryOnServerErrors(maxRetries = 5)
        exponentialDelay()
    }
}
