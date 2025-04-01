package no.nav.tsm.syk_inn_api.client

import no.nav.tsm.syk_inn_api.exception.BehandlerNotFoundException
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
    private val webClient: WebClient =
        webClientBuilder.baseUrl(baseUrl).build()

    fun getSykmelderByHpr(behandlerHpr: String, sykmeldingId: String): Sykmelder {
        return webClient.get()
            .uri { uriBuilder -> uriBuilder.path("/api/v2/behandlerMedHprNummer").build() }
            .headers {
                it.set("Content-Type", "application/json")
                it.set("Nav-CallId", sykmeldingId)
                it.set("hprNummer", behandlerHpr)
            }
            .retrieve()
            .onStatus({ it.isError }) { res -> onStatusError(res) }
            .bodyToMono(Sykmelder::class.java)
            .block() ?: throw BehandlerNotFoundException("Body is not Behandler")
    }

    private fun onStatusError(res: ClientResponse): Nothing {
        throw RuntimeException("Error from syfohelsenettproxy got status code: ${res.statusCode()}")
            .also { logger.error(it.message, it) }
    }
}
