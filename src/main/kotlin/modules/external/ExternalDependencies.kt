package modules.external

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.external.clients.pdl.PdlClient
import modules.external.clients.texas.TexasClient
import no.nav.tsm.core.dynamicDependencies
import no.nav.tsm.modules.external.clients.pdl.PdlLocalClient
import no.nav.tsm.modules.external.clients.texas.TexasLocalClient

fun Application.configureExternalDependencies() {
    val baseHttpClient: HttpClient by dependencies

    dependencies { provide("RetryHttpClient") { createExternalApiHttpClient(baseHttpClient) } }

    dynamicDependencies {
        local {
            provide(TexasLocalClient::class)
            provide(PdlLocalClient::class)
        }
        cloud {
            provide(TexasClient::class)
            provide(PdlClient::class)
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
