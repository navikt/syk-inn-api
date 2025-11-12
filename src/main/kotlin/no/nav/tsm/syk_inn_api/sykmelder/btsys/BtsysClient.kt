package no.nav.tsm.syk_inn_api.sykmelder.btsys

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import java.util.*
import no.nav.tsm.syk_inn_api.security.TexasClient
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

interface IBtsysClient {
    fun checkSuspensionStatus(sykmelderFnr: String, oppslagsdato: LocalDate): Result<Suspendert>
}

@Profile("!local & !test")
@Component
class BtsysClient(
    restClientBuilder: RestClient.Builder,
    private val texasClient: TexasClient,
    @param:Value($$"${services.external.btsys.url}") private val btsysEndpointUrl: String,
) : IBtsysClient {
    private val restClient: RestClient = restClientBuilder.baseUrl(btsysEndpointUrl).build()
    private val logger = logger()

    @WithSpan
    override fun checkSuspensionStatus(
        sykmelderFnr: String,
        oppslagsdato: LocalDate
    ): Result<Suspendert> {
        val (accessToken) = this.getToken()

        val loggId = UUID.randomUUID().toString()
        return try {
            val response =
                restClient
                    .get()
                    .uri { uriBuilder ->
                        uriBuilder
                            .path("/api/v1/suspensjon/status")
                            .queryParam("oppslagsdato", oppslagsdato.toString())
                            .build()
                    }
                    .headers {
                        it.set("Nav-Call-Id", loggId)
                        it.set("Nav-Consumer-Id", "syk-inn-api")
                        it.set("Nav-Personident", sykmelderFnr)
                        it.set("Authorization", "Bearer $accessToken")
                        it.set("Accept", "application/json")
                    }
                    .retrieve()
                    .body(Suspendert::class.java)

            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(IllegalStateException("Btsys returned no suspension status"))
            }
        } catch (e: RestClientResponseException) {
            val span = Span.current()
            span.setStatus(StatusCode.ERROR)

            val status = e.statusCode
            val body = e.responseBodyAsString
            logger.error("BtsysClient request failed with ${status.value()} and body: $body", e)
            val exception =
                IllegalStateException("HelsenettProxy error (${status.value()}): $body", e)
            span.recordException(exception)
            Result.failure(exception)
        } catch (e: Exception) {
            logger.error("Error while calling Btsys API", e)

            val span = Span.current()
            span.setStatus(StatusCode.ERROR)
            span.recordException(e)

            Result.failure(e)
        }
    }

    private fun getToken(): TexasClient.TokenResponse =
        texasClient.requestToken("team-rocket", "btsys-api")
}

data class Suspendert(val suspendert: Boolean)
