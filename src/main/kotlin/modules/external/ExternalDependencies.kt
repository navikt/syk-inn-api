package modules.external

import core.dynamicDependencies
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.external.clients.pdl.PdlCloudClient
import modules.external.clients.pdl.PdlLocalClient
import modules.external.clients.texas.TexasCloudClient
import modules.external.clients.texas.TexasLocalClient

fun Application.configureExternalDependencies() {
    val baseHttpClient: HttpClient by dependencies

    dependencies {
        provide<HttpClient>("RetryHttpClient") { createExternalApiHttpClient(baseHttpClient) }
    }

    dynamicDependencies {
        local {
            provide(TexasLocalClient::class)
            provide(PdlLocalClient::class)
        }
        cloud {
            provide(TexasCloudClient::class)
            provide(PdlCloudClient::class)
        }
    }
}

private fun createExternalApiHttpClient(baseHttpClient: HttpClient) =
    baseHttpClient.config {
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            exponentialDelay()
        }
    }
