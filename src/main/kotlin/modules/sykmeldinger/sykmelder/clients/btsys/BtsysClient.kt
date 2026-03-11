package modules.sykmeldinger.sykmelder.clients.btsys

import core.Environment
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.callid.*
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.*
import io.ktor.server.plugins.di.annotations.*
import java.time.LocalDate
import plugins.auth.TexasClient

data class BtsysResponse(val suspendert: Boolean)

class BtsysException(message: String, cause: Exception? = null) : Exception(message, cause)

sealed interface BtsysClient {
    suspend fun isSuspendert(sykmelderIdent: String, oppslagsdato: LocalDate): Boolean?
}

class BtsysCloudClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    private val texasClient: TexasClient,
    private val environment: Environment,
) : BtsysClient {
    private val httpClient: HttpClient =
        httpClient.config {
            install(CallId) {
                intercept { request, callId -> request.header("Nav-Call-Id", callId) }
            }
        }

    override suspend fun isSuspendert(sykmelderIdent: String, oppslagsdato: LocalDate): Boolean? {
        val (accessToken) = this.getToken()

        val response =
            httpClient.get("${environment.external().btsys}/api/v1/suspensjon/status") {
                headers {
                    append("Content-Type", "application/json")
                    append("Nav-Consumer-Id", "syk-inn-api")
                    append("Nav-Personident", sykmelderIdent)
                    append("Authorization", "Bearer $accessToken")
                }
            }

        return when {
            response.status.isSuccess() -> {
                val result: BtsysResponse = response.body<BtsysResponse>()

                result.suspendert
            }

            response.status == HttpStatusCode.NotFound -> null

            else -> {
                // TODO Logg feil respons og ident i teamlogg
                // TODO handter andre feil enn berre not found.
                throw BtsysException("Btysys responded with status ${response.status}")
            }
        }
    }

    suspend fun getToken() = texasClient.requestToken("team-rocket", "btsys-api")
}
