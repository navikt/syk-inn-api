package modules.sykmeldinger.sykmelder

import core.dynamicDependencies
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.server.application.Application
import io.ktor.server.plugins.di.dependencies
import modules.sykmeldinger.sykmelder.clients.btsys.BtsysCloudClient
import modules.sykmeldinger.sykmelder.clients.btsys.BtsysLocalClient
import modules.sykmeldinger.sykmelder.clients.hpr.HprCloudClient
import modules.sykmeldinger.sykmelder.clients.hpr.HprLocalClient

fun Application.configureSykmelderDependencies() {
    val baseHttpClient: HttpClient by dependencies

    dependencies {
        provide<HttpClient>("RetryHttpClient") { createExternalApiHttpClient(baseHttpClient) }
    }

    dynamicDependencies {
        local {
            provide(BtsysLocalClient::class)
            provide(HprLocalClient::class)
        }
        cloud {
            provide(BtsysCloudClient::class)
            provide(HprCloudClient::class)
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
