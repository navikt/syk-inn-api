package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.callid.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.http.*
import io.ktor.serialization.jackson.jackson
import io.ktor.server.plugins.di.annotations.*
import java.time.LocalDate
import no.nav.tsm.core.Environment
import no.nav.tsm.core.logger
import no.nav.tsm.plugins.auth.TexasClient

class BtsysCloudClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    private val texasClient: TexasClient,
    private val environment: Environment,
) : BtsysClient {
    private val logger = logger()

    private val httpClient: HttpClient = httpClient.config {
        install(CallId) { intercept { request, callId -> request.header("Nav-Call-Id", callId) } }
        install(ContentNegotiation) { jackson {} }
    }

    data class BtsysResponse(val suspendert: Boolean)

    override suspend fun isSuspendert(
        sykmelderIdent: String,
        oppslagsdato: LocalDate,
    ): Either<BtsysClient.SuspendertErrors, Boolean> {
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

                result.suspendert.right()
            }

            response.status == HttpStatusCode.NotFound -> {
                BtsysClient.SuspendertErrors.NotFound.left()
            }

            else -> {
                logger.error("Btysys responded with status ${response.status}")
                BtsysClient.SuspendertErrors.UnknownError.left()
            }
        }
    }

    suspend fun getToken() = texasClient.requestToken("team-rocket", "btsys-api")
}
