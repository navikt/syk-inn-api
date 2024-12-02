package no.nav.tsm.sykinnapi.client.syfosmregler

import no.nav.tsm.sykinnapi.config.token.M2MTokenService
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Component
class SyfosmreglerClient(
    syfosmreglerM2mWebBuilder: WebClient.Builder,
    @Value("\${syfosmregler.url}") syfosmreglerBaseUrl: String,
    private val m2mTokenService: M2MTokenService
) {
    private val logger = LoggerFactory.getLogger(SyfosmreglerClient::class.java)

    private val webClient = syfosmreglerM2mWebBuilder.baseUrl(syfosmreglerBaseUrl).build()

    fun validate(receivedSykmelding: ReceivedSykmelding): ValidationResult {

        val responseValidationResult =
            webClient
                .post()
                .uri { uriBuilder -> uriBuilder.path("/v1/rules/validate").build() }
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header("Nav-CallId", receivedSykmelding.sykmelding.id)
                .header(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer ${m2mTokenService.getM2MToken("syfosmregler-m2m")}"
                )
                .bodyValue(receivedSykmelding)
                .retrieve()
                .onStatus({ status -> status.is4xxClientError || status.is5xxServerError }) {
                    response ->
                    response.createException().flatMap {
                        logger.error("Feil ved validering status: ${response.statusCode()}")
                        Mono.error(RuntimeException("Feil validering: ${response.statusCode()}"))
                    }
                }
                .bodyToFlux(ValidationResult::class.java)
                .blockFirst()

        return responseValidationResult
    }
}
