package no.nav.tsm.sykinnapi.client

import no.nav.tsm.sykinnapi.config.M2MTokenService
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class SyfohelsenettproxyClient(
    syfohelsenettproxyM2mWebBuilder: WebClient.Builder,
    @Value("\${syfohelsenettproxy.url}") syfohelsenettproxyBaseUrl: String,
    private val m2mTokenService: M2MTokenService
) {
    private val logger = LoggerFactory.getLogger(SyfohelsenettproxyClient::class.java)

    private val webClient =
        syfohelsenettproxyM2mWebBuilder.baseUrl(syfohelsenettproxyBaseUrl).build()

    fun getBehandlerByHpr(behandlerHpr: String, sykmeldingId: String): Behandler {

        val responseBehandler =
            webClient
                .get()
                .uri { uriBuilder -> uriBuilder.path("/api/v2/behandlerMedHprNummer").build() }
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("Nav-CallId", sykmeldingId)
                .header("hprNummer", behandlerHpr)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${m2mTokenService.getM2MToken("syfohelsenettproxy-m2m")}"
                )
                .retrieve()
                .onStatus({ status -> status.is4xxClientError || status.is5xxServerError }) {
                    response ->
                    response.createException().flatMap {
                        logger.error(
                            "Feil ved henting av behandlerMedHprNummer status: ${response.statusCode()}"
                        )
                        Mono.error(
                            RuntimeException(
                                "Feil ved henting av behandlerMedHprNummer: ${response.statusCode()}"
                            )
                        )
                    }
                }
                .bodyToFlux(Behandler::class.java)
                .blockFirst()

        return responseBehandler
    }
}
