package no.nav.tsm.syk_inn_api.client

import no.nav.tsm.syk_inn_api.exception.HelsenettProxyException
import no.nav.tsm.syk_inn_api.model.Sykmelder
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient

@Component
class HelsenettProxyClient(
    webClientBuilder: WebClient.Builder,
    @Value("\${syfohelsenettproxy.base-url}") private val baseUrl: String
) {
    private val logger = LoggerFactory.getLogger(HelsenettProxyClient::class.java)
    private val webClient: WebClient = webClientBuilder.baseUrl(baseUrl).build()

    sealed class Result<out T> {
        data class Success<out T>(val data: T) : Result<T>()

        data class Failure(val error: Throwable) : Result<Nothing>()
    }

    fun getSykmelderByHpr(behandlerHpr: String, sykmeldingId: String): Result<Sykmelder> {
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
                Result.Failure(HelsenettProxyException("HelseNettProxy returned no Sykmelder"))
            }
        } catch (e: Exception) {
            logger.error("Error while calling HelseNettProxy API", e)
            Result.Failure(e)
        }
    }

    private fun onStatusError(res: ClientResponse): Nothing {
        throw RuntimeException("Error from syfohelsenettproxy got status code: ${res.statusCode()}")
            .also { logger.error(it.message, it) }
    }
}
