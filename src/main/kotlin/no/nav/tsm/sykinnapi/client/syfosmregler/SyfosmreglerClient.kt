package no.nav.tsm.sykinnapi.client.syfosmregler

import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class SyfosmreglerClient(
    syfosmreglerM2mRestClientBuilder: RestClient.Builder,
    @Value("\${syfosmregler.url}") syfosmreglerBaseUrl: String,
    private val handler: RestClient.ResponseSpec.ErrorHandler
) {
    private val restClient = syfosmreglerM2mRestClientBuilder.baseUrl(syfosmreglerBaseUrl).build()

    fun validate(receivedSykmelding: ReceivedSykmelding): ValidationResult =
        restClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/v1/rules/validate").build() }
            .attributes(clientRegistrationId("syfosmregler-m2m"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("Nav-CallId", receivedSykmelding.sykmelding.id)
            .retrieve()
            .onStatus({ it.isError }) { req, res -> handler.handle(req, res) }
            .body<ValidationResult>()
            ?: throw RuntimeException("Strange Error")
}
