package no.nav.tsm.syk_inn_api.person.pdl

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.syk_inn_api.security.TexasClient
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientResponseException

interface IPdlClient {
    fun getPerson(fnr: String): Result<PdlPerson>
}

@Profile("!local & !test")
@Component
class PdlClient(
    restClientBuilder: RestClient.Builder,
    private val texasClient: TexasClient,
    @param:Value($$"${services.teamsykmelding.pdlcache.url}") private val pdlEndpointUrl: String,
) : IPdlClient {
    private val restClient = restClientBuilder.baseUrl(pdlEndpointUrl).build()
    private val logger = logger()
    private val teamLogger = teamLogger()

    @WithSpan("PdlClient.getPerson")
    override fun getPerson(fnr: String): Result<PdlPerson> {
        val (accessToken) = getToken()

        return try {
            val response =
                restClient
                    .get()
                    .uri { uriBuilder -> uriBuilder.path("/api/person").build() }
                    .headers {
                        it.set("Nav-Consumer-Id", "syk-inn-api")
                        it.set("Authorization", "Bearer $accessToken")
                        it.set("Ident", fnr)
                    }
                    .retrieve()
                    .body(PdlPerson::class.java)
            if (response != null) {
                Result.success(response)
            } else {
                Result.failure(IllegalStateException("Pdl cache did not return a person"))
            }
        } catch (e: RestClientResponseException) {
            val span = Span.current()
            span.setStatus(StatusCode.ERROR)

            val status = e.statusCode
            val body = e.responseBodyAsString

            when {
                status.value() == 404 -> {
                    teamLogger.warn("Person with fnr $fnr not found in PDL cache. Body: $body", e)
                    logger.warn("PDL person not found in PDL cache", e)
                    val exception = IllegalStateException("Could not find person in pdl cache")
                    span.recordException(exception)
                    Result.failure(exception)
                }
                status.is4xxClientError -> {
                    teamLogger.error("PDL client error ${status.value()}: $body, fnr: $fnr", e)
                    val exception =
                        IllegalStateException("PDL client error (${status.value()}): $body")
                    span.recordException(exception)
                    Result.failure(exception)
                }
                status.is5xxServerError -> {
                    teamLogger.error("PDL server error ${status.value()}: $body, fnr: $fnr", e)
                    val exception =
                        IllegalStateException("PDL server error (${status.value()}): $body")
                    span.recordException(exception)
                    Result.failure(
                        exception,
                    )
                }
                else -> {
                    teamLogger.error(
                        "PDL unexpected HTTP status ${status.value()}: $body, fnr: $fnr",
                        e,
                    )
                    val exception =
                        IllegalStateException("PDL unexpected status (${status.value()}): $body")
                    span.recordException(exception)
                    Result.failure(
                        exception,
                    )
                }
            }
        } catch (e: Exception) {
            val span = Span.current()
            span.setStatus(StatusCode.ERROR)
            span.recordException(e)

            logger.error("Error while calling Pdl API", e)
            Result.failure(e)
        }
    }

    private fun getToken(): TexasClient.TokenResponse =
        texasClient.requestToken("tsm", "tsm-pdl-cache")
}
