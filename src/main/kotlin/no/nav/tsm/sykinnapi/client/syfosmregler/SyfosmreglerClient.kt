package no.nav.tsm.sykinnapi.client.syfosmregler

import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class SyfosmreglerClient(
    @Qualifier("syfosmreglerClient") private val syfosmreglerClient: RestClient,
) {

    private val logger = LoggerFactory.getLogger(SyfosmreglerClient::class.java)

    fun validate(receivedSykmelding: ReceivedSykmelding): ValidationResult =
        syfosmreglerClient
            .post()
            .uri { uriBuilder -> uriBuilder.path("/v1/rules/validate").build() }
            .attributes(clientRegistrationId("syfosmregler-m2m"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("Nav-CallId", receivedSykmelding.sykmelding.id)
            .retrieve()
            .onStatus({ it.isError }) { req, res -> onStatusError(res) }
            .body<ValidationResult>()
            ?: throw RuntimeException("Body is not ValidationResult")

    private fun onStatusError(res: ClientHttpResponse) {
        throw RuntimeException("Error got statuscode: ${res.statusCode}").also {
            logger.error(it.message, it)
        }
    }
}
