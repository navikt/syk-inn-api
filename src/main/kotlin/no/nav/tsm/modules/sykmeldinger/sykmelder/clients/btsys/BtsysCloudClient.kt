package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.callid.*
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.*
import io.ktor.serialization.jackson.jackson
import io.ktor.server.plugins.di.annotations.*
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import kotlin.collections.mapOf
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
        install(ContentNegotiation) {
            jackson { configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false) }
        }
    }

    data class BtsysResponse(val suspendert: Boolean)

    @WithSpan
    override suspend fun isSuspendert(
        sykmelderIdent: String,
        oppslagsdato: LocalDate,
    ): Either<BtsysClient.SuspendertErrors, Boolean> {
        val span = Span.current()
        val (accessToken) = this.getToken()

        val response =
            httpClient.post("${environment.external().btsys}/api/v1/suspensjon/soek") {
                headers {
                    append("Nav-Consumer-Id", "syk-inn-api")
                    append("Authorization", "Bearer $accessToken")
                }
                contentType(ContentType.Application.Json)
                setBody(
                    mapOf(
                        "personident" to sykmelderIdent,
                        "oppslagsdato" to oppslagsdato.toString(),
                    )
                )
            }

        return when {
            response.status.isSuccess() -> {
                span.setAttribute("client.outcome", "ok")
                val result: BtsysResponse = response.body<BtsysResponse>()

                result.suspendert.right()
            }

            response.status == HttpStatusCode.NotFound -> {
                span.setAttribute("client.outcome", "not-found")
                BtsysClient.SuspendertErrors.NotFound.left()
            }

            else -> {
                span.setAttribute("client.outcome", response.status.toString())
                logger.error("Btsys responded with status ${response.status}")
                BtsysClient.SuspendertErrors.UnknownError.left()
            }
        }
    }

    suspend fun getToken() = texasClient.requestToken("team-rocket", "btsys-api")
}
