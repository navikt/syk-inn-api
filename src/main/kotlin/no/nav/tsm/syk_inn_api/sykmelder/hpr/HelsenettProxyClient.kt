package no.nav.tsm.syk_inn_api.sykmelder.hpr

import no.nav.tsm.syk_inn_api.security.TexasClient
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.secureLogger
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient

interface IHelsenettProxyClient {
    fun getSykmelderByHpr(behandlerHpr: String, callId: String): Result<HprSykmelder>
}

@Profile("!local & !test")
@Component
class HelsenettProxyClient(
    webClientBuilder: WebClient.Builder,
    private val texasClient: TexasClient,
    @param:Value($$"${services.teamsykmelding.syfohelsenettproxy.url}") private val baseUrl: String,
) : IHelsenettProxyClient {
    private val logger = logger()
    private val secureLog = secureLogger()

    private val webClient: WebClient = webClientBuilder.baseUrl(baseUrl).build()

    override fun getSykmelderByHpr(
        behandlerHpr: String,
        // TODO: Use MDC?
        callId: String
    ): Result<HprSykmelder> {
        val (accessToken) = getToken()

        logger.info(
            "Getting sykmelder for hpr=$behandlerHpr, sykmeldingId=$callId",
        )

        return try {
            val response =
                webClient
                    .get()
                    .uri { uriBuilder -> uriBuilder.path("/api/v2/behandlerMedHprNummer").build() }
                    .headers {
                        it.set("Content-Type", "application/json")
                        it.set("Nav-CallId", callId)
                        it.set("hprNummer", behandlerHpr)
                        it.set("Authorization", "Bearer $accessToken")
                    }
                    .retrieve()
                    .onStatus({ it.isError }) { res -> onStatusError(res) }
                    .bodyToMono(HprSykmelder::class.java)
                    .block()

            if (response != null) {
                logger.info(
                    "Response from HelsenettProxy was successful for sykmeldingId=$callId",
                )
                Result.success(response)
            } else {
                val msg = "HelsenettProxy returned null response for sykmeldingId=$callId"
                logger.warn(msg)
                secureLog.warn("$msg and hpr=$behandlerHpr")
                Result.failure(IllegalStateException("HelsenettProxy returned no Sykmelder"))
            }
        } catch (e: Exception) {
            logger.error("Error while calling HelsenettProxy API for sykmeldingId=$callId", e)
            secureLog.error("Exception with hpr=$behandlerHpr for sykmeldingId=$callId", e)
            Result.failure(e)
        }
    }

    private fun onStatusError(res: ClientResponse): Nothing {
        throw RuntimeException("Error from syfohelsenettproxy got status code: ${res.statusCode()}")
            .also { logger.error(it.message, it) }
    }

    fun getToken(): TexasClient.TokenResponse =
        texasClient.requestToken("teamsykmelding", "syfohelsenettproxy")
}
