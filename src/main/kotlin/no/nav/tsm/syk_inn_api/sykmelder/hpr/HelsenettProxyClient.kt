package no.nav.tsm.syk_inn_api.sykmelder.hpr

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

class HprException(message: String, cause: Exception?) : Exception(message, cause)

interface IHelsenettProxyClient {
    fun getSykmelderByHpr(behandlerHpr: String, callId: String): Result<HprSykmelder>

    fun getSykmelderByFnr(behandlerFnr: String, callId: String): Result<HprSykmelder>
}

@Profile("!local & !test")
@Component
class HelsenettProxyClient(
    restClientBuilder: RestClient.Builder,
    private val texasClient: TexasClient,
    @param:Value($$"${services.teamsykmelding.syfohelsenettproxy.url}") private val baseUrl: String,
) : IHelsenettProxyClient {
    private val logger = logger()
    private val teamLogger = teamLogger()

    private val restClient: RestClient = restClientBuilder.baseUrl(baseUrl).build()

    @WithSpan("Helsenettproxy.getSykmelderByHpr")
    override fun getSykmelderByHpr(
        behandlerHpr: String,
        // TODO: Use MDC?
        callId: String
    ): Result<HprSykmelder> {
        val (accessToken) = getToken()

        return try {
            val response =
                restClient
                    .get()
                    .uri { uriBuilder -> uriBuilder.path("/api/v2/behandlerMedHprNummer").build() }
                    .headers {
                        it.set("Content-Type", "application/json")
                        it.set("Nav-CallId", callId)
                        it.set("hprNummer", behandlerHpr)
                        it.set("Authorization", "Bearer $accessToken")
                    }
                    .retrieve()
                    .body(HprSykmelder::class.java)

            if (response != null) {
                Result.success(response)
            } else {
                val msg = "HelsenettProxy returned null response for sykmeldingId=$callId"
                logger.warn(msg)
                teamLogger.warn("$msg and hpr=$behandlerHpr")
                Result.failure(
                    IllegalStateException("HelsenettProxy returned no Sykmelder").failSpan()
                )
            }
        } catch (e: RestClientResponseException) {
            val status = e.statusCode
            val body = e.responseBodyAsString
            teamLogger.error(
                "HelsenettProxy HTTP ${status.value()}: $body, hpr=$behandlerHpr, callId=$callId",
                e,
            )
            logger.error(
                "HelsenettProxy request failed with ${status.value()} for sykmeldingId=$callId",
                e,
            )
            if (status.value() == 404) {
                return Result.failure(
                    HprException("Fant ikke behandler for hpr=$behandlerHpr", e).failSpan()
                )
            }

            Result.failure(
                IllegalStateException(
                        "HelsenettProxy error (${status.value()}): $body",
                        e,
                    )
                    .failSpan(),
            )
        } catch (e: Exception) {
            logger.error("Error while calling HelsenettProxy API for sykmeldingId=$callId", e)
            teamLogger.error("Exception with hpr=$behandlerHpr for sykmeldingId=$callId", e)
            Result.failure(e.failSpan())
        }
    }

    @WithSpan("Helsenettproxy.getSykmelderByFnr")
    override fun getSykmelderByFnr(behandlerFnr: String, callId: String): Result<HprSykmelder> {
        val (accessToken) = getToken()

        return try {
            val response =
                restClient
                    .get()
                    .uri { uriBuilder -> uriBuilder.path("/api/v2/behandler").build() }
                    .headers {
                        it.set("Content-Type", "application/json")
                        it.set("Nav-CallId", callId)
                        it.set("behandlerFnr", behandlerFnr)
                        it.set("Authorization", "Bearer $accessToken")
                    }
                    .retrieve()
                    .body(HprSykmelder::class.java)

            if (response != null) {
                Result.success(response)
            } else {
                val msg = "HelsenettProxy returned null response for sykmeldingId=$callId"
                logger.warn(msg)
                teamLogger.warn("$msg and hpr=$behandlerFnr")
                Result.failure(
                    IllegalStateException("HelsenettProxy returned no Sykmelder").failSpan()
                )
            }
        } catch (e: RestClientResponseException) {
            val status = e.statusCode
            val body = e.responseBodyAsString
            teamLogger.error(
                "HelsenettProxy HTTP ${status.value()}: $body, fnr=$behandlerFnr, callId=$callId",
                e,
            )
            logger.error(
                "HelsenettProxy request failed with ${status.value()} for sykmeldingId=$callId",
                e,
            )

            val exception =
                if (status.value() == 404) {
                    HprException("Fant ikke behandler for fnr", e)
                } else {
                    IllegalStateException("HelsenettProxy error (${status.value()}): $body", e)
                }

            return Result.failure(exception.failSpan())
        } catch (e: Exception) {
            logger.error("Error while calling HelsenettProxy API for sykmeldingId=$callId", e)
            teamLogger.error("Exception with hpr=$behandlerFnr for sykmeldingId=$callId", e)
            Result.failure(e.failSpan())
        }
    }

    fun getToken(): TexasClient.TokenResponse =
        texasClient.requestToken("teamsykmelding", "syfohelsenettproxy")
}
