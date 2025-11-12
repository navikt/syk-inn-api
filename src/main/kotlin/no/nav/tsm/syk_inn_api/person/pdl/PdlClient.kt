package no.nav.tsm.syk_inn_api.person.pdl

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.syk_inn_api.security.TexasClient
import no.nav.tsm.syk_inn_api.utils.failSpan
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
                Result.failure(
                    IllegalStateException("Pdl cache did not return a person").failSpan()
                )
            }
        } catch (e: RestClientResponseException) {
            val status = e.statusCode
            val body = e.responseBodyAsString

            when {
                status.value() == 404 -> {
                    teamLogger.warn("Person with fnr $fnr not found in PDL cache. Body: $body", e)
                    logger.warn("PDL person not found in PDL cache", e)
                    Result.failure(
                        IllegalStateException("Could not find person in pdl cache").failSpan(),
                    )
                }
                status.is4xxClientError -> {
                    teamLogger.error("PDL client error ${status.value()}: $body, fnr: $fnr", e)
                    Result.failure(
                        IllegalStateException("PDL client error (${status.value()}): $body")
                            .failSpan()
                    )
                }
                status.is5xxServerError -> {
                    teamLogger.error("PDL server error ${status.value()}: $body, fnr: $fnr", e)
                    Result.failure(
                        IllegalStateException("PDL server error (${status.value()}): $body")
                            .failSpan(),
                    )
                }
                else -> {
                    teamLogger.error(
                        "PDL unexpected HTTP status ${status.value()}: $body, fnr: $fnr",
                        e,
                    )
                    Result.failure(
                        IllegalStateException("PDL unexpected status (${status.value()}): $body")
                            .failSpan(),
                    )
                }
            }
        } catch (e: Exception) {
            logger.error("Error while calling Pdl API", e)
            Result.failure(e.failSpan())
        }
    }

    private fun getToken(): TexasClient.TokenResponse =
        texasClient.requestToken("tsm", "tsm-pdl-cache")
}
