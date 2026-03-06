package modules.sykmelder

import core.dynamicDependencies
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.sykmelder.clients.btsys.BtsysCloudClient
import modules.sykmelder.clients.btsys.BtsysLocalClient
import modules.sykmelder.clients.hpr.HprCloudClient
import modules.sykmelder.clients.hpr.HprLocalClient
import modules.sykmelder.clients.pdl.PdlCloudClient
import modules.sykmelder.clients.pdl.PdlLocalClient
import modules.sykmelder.clients.texas.TexasCloudClient
import modules.sykmelder.clients.texas.TexasLocalClient

fun Application.configureSykmelderDependencies() {
    val baseHttpClient: HttpClient by dependencies

    dependencies {
        provide<HttpClient>("RetryHttpClient") { createExternalApiHttpClient(baseHttpClient) }
    }

    dynamicDependencies {
        local {
            provide(TexasLocalClient::class)
            provide(BtsysLocalClient::class)
            provide(HprLocalClient::class)
            provide(PdlLocalClient::class)
        }
        cloud {
            provide(TexasCloudClient::class)
            provide(BtsysCloudClient::class)
            provide(HprCloudClient::class)
            provide(PdlCloudClient::class)
        }
    }

    dependencies { provide<SykmelderService>(SykmelderService::class) }
}

private fun createExternalApiHttpClient(baseHttpClient: HttpClient) =
    baseHttpClient.config {
        install(HttpRequestRetry) {
            retryOnServerErrors(maxRetries = 5)
            exponentialDelay()
        }
    }
