package no.nav.tsm.sykinnapi.client.syfohelsenettproxy

import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders.CONTENT_TYPE
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class SyfohelsenettproxyClient(
    @Qualifier("syfohelsenettproxyClientRestClient")
    private val syfohelsenettproxyClient: RestClient,
) {
    private val logger = LoggerFactory.getLogger(SyfohelsenettproxyClient::class.java)

    fun getBehandlerByHpr(behandlerHpr: String, sykmeldingId: String) =
        syfohelsenettproxyClient
            .get()
            .uri { uriBuilder -> uriBuilder.path("/api/v2/behandlerMedHprNummer").build() }
            .attributes(clientRegistrationId("syfohelsenettproxy-m2m"))
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("Nav-CallId", sykmeldingId)
            .header("hprNummer", behandlerHpr)
            .retrieve()
            .onStatus({ it.isError }) { req, res -> onStatusError(res) }
            .body<Behandler>()
            ?: throw RuntimeException("Body is not Behandler")

    private fun onStatusError(res: ClientHttpResponse): Nothing {
        throw RuntimeException("Error from syfohelsenettproxy got statuscode: ${res.statusCode}")
            .also { logger.error(it.message, it) }
    }
}
