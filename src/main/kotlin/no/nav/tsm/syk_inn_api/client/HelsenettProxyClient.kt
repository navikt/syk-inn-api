package no.nav.tsm.syk_inn_api.client

import no.nav.tsm.syk_inn_api.exception.HelsenettProxyException
import no.nav.tsm.syk_inn_api.model.Sykmelder
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
    @Value("\${syfohelsenettproxy.base-url}") private val baseUrl: String
) : IHelsenettProxyClient {
    private val logger = LoggerFactory.getLogger(HelsenettProxyClient::class.java)
    private val webClient: WebClient = webClientBuilder.baseUrl(baseUrl).build()

    init {
        println("HelsenettProxyClient initialized with base URL: $baseUrl")
    }
    override fun getSykmelderByHpr(behandlerHpr: String, sykmeldingId: String): Result<Sykmelder> {
        return try {
            val response =
                webClient
                    .get()
                    .uri { uriBuilder -> uriBuilder.path("/api/v2/behandlerMedHprNummer").build() }
                    .headers {
                        it.set("Content-Type", "application/json")
                        it.set("Nav-CallId", sykmeldingId)
                        it.set("hprNummer", behandlerHpr)
                    }
                    .retrieve()
                    .onStatus({ it.isError }) { res -> onStatusError(res) }
                    .bodyToMono(Sykmelder::class.java)
                    .block()

            if (response != null) {
                Result.Success(response)
            } else {
                Result.Failure(HelsenettProxyException("HelsenettProxy returned no Sykmelder"))
            }
        } catch (e: Exception) {
            logger.error("Error while calling HelsenettProxy API", e)
            Result.Failure(e)
        }
    }

    private fun onStatusError(res: ClientResponse): Nothing {
        throw RuntimeException("Error from syfohelsenettproxy got status code: ${res.statusCode()}")
            .also { logger.error(it.message, it) }
    }
}
