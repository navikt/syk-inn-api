package modules.sykmelder.clients.btsys

import io.ktor.client.HttpClient
import io.ktor.client.plugins.callid.CallId
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.server.plugins.di.annotations.Named

interface BtsysClient {
    suspend fun isSuspendert(hpr: String): Boolean
}

class BtsysCloudClient(@Named("RetryHttpClient") httpClient: HttpClient) : BtsysClient {
    private val httpClient: HttpClient =
        httpClient.config {
            install(CallId) {
                intercept { request, callId -> request.header("Nav-Call-Id", callId) }
            }
        }

    override suspend fun isSuspendert(hpr: String): Boolean {
        val result = httpClient.get("http://example.com")
        TODO("BTSYS Stub - implementer kall mot BTSYS for å sjekke om sykmelder er suspendert")
    }
}
