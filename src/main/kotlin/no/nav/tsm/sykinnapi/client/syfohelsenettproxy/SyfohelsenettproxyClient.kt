package no.nav.tsm.sykinnapi.client.syfohelsenettproxy

import no.nav.tsm.sykinnapi.client.RestClientConfiguration.LoggingErrorHandler
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders.*
import org.springframework.http.MediaType.*
import org.springframework.security.oauth2.client.web.client.RequestAttributeClientRegistrationIdResolver.clientRegistrationId
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class SyfohelsenettproxyClient(
    syfohelsenettproxyM2mRestClientBuilder: RestClient.Builder,
    @Value("\${syfohelsenettproxy.url}") syfohelsenettproxyBaseUrl: String,
    private val handler: RestClient.ResponseSpec.ErrorHandler = LoggingErrorHandler()
) {
    private val restClient =
        syfohelsenettproxyM2mRestClientBuilder.baseUrl(syfohelsenettproxyBaseUrl).build()

    fun getBehandlerByHpr(behandlerHpr: String, sykmeldingId: String) =
        restClient
            .get()
            .uri { uriBuilder -> uriBuilder.path("/api/v2/behandlerMedHprNummer").build() }
            .attributes(clientRegistrationId("syfohelsenettproxy-m2m"))
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("Nav-CallId", sykmeldingId)
            .header("hprNummer", behandlerHpr)
            .retrieve()
            .onStatus({ it.isError }) { req, res -> handler.handle(req, res) }
            .body<Behandler>()
            ?: throw RuntimeException("Strange Error")
}
