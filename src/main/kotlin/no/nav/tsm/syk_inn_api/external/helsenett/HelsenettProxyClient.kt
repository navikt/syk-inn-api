package no.nav.tsm.syk_inn_api.external.helsenett

import no.nav.tsm.syk_inn_api.common.Result
import no.nav.tsm.syk_inn_api.common.Sykmelder
import no.nav.tsm.syk_inn_api.exception.HelsenettProxyException
import no.nav.tsm.syk_inn_api.external.token.TokenService
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient

interface IHelsenettProxyClient {
    fun getSykmelderByHpr(behandlerHpr: String, sykmeldingId: String): Result<Sykmelder>
}

@Profile("!local")
@Component
class HelsenettProxyClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${syfohelsenettproxy.base-url}") private val baseUrl: String,
    private val tokenService: TokenService,
) : IHelsenettProxyClient {
    private val logger = LoggerFactory.getLogger(HelsenettProxyClient::class.java)
    private val webClient: WebClient = webClientBuilder.baseUrl(baseUrl).build()
    private val secureLog: Logger = LoggerFactory.getLogger("securelog")

    override fun getSykmelderByHpr(behandlerHpr: String, sykmeldingId: String): Result<Sykmelder> {
        val accessToken = tokenService.getTokenForHelsenettProxy().access_token

        return try {
            val response =
                webClient
                    .get()
                    .uri { uriBuilder -> uriBuilder.path("/api/v2/behandlerMedHprNummer").build() }
                    .headers {
                        it.set("Content-Type", "application/json")
                        it.set("Nav-CallId", sykmeldingId)
                        it.set("hprNummer", behandlerHpr)
                        it.set("Authorization", "Bearer $accessToken")
                    }
                    .retrieve()
                    .onStatus({ it.isError }) { res -> onStatusError(res) }
                    .bodyToMono(Sykmelder::class.java)
                    .block()

            if (response != null) {
                logger.info(
                    "Response from HelsenettProxy was successful for sykmeldingId=$sykmeldingId"
                )
                Result.Success(response)
            } else {
                val msg = "HelsenettProxy returned null response for sykmeldingId=$sykmeldingId"
                logger.warn(msg)
                secureLog.warn("$msg and hpr=$behandlerHpr")
                Result.Failure(HelsenettProxyException("HelsenettProxy returned no Sykmelder"))
            }
        } catch (e: Exception) {
            logger.error("Error while calling HelsenettProxy API for sykmeldingId=$sykmeldingId", e)
            secureLog.error("Exception with hpr=$behandlerHpr for sykmeldingId=$sykmeldingId", e)
            Result.Failure(e)
        }
    }

    private fun onStatusError(res: ClientResponse): Nothing {
        throw RuntimeException("Error from syfohelsenettproxy got status code: ${res.statusCode()}")
            .also { logger.error(it.message, it) }
    }
}
