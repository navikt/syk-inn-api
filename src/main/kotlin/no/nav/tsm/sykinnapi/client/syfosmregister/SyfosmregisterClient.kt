package no.nav.tsm.sykinnapi.client.syfosmregister

import no.nav.tsm.sykinnapi.modell.syfosmregister.SyfoSmRegisterSykmelding
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
class SyfosmregisterClient(
    @Qualifier("syfosmregisterClientRestClient") private val syfosmregisterClient: RestClient,
) {
    private val logger = LoggerFactory.getLogger(SyfosmregisterClient::class.java)

    fun getSykmelding(sykmeldingId: String): SyfoSmRegisterSykmelding =
        syfosmregisterClient
            .get()
            .uri { uriBuilder ->
                uriBuilder.path("/api/v2/sykmelding/sykinn/${sykmeldingId}").build()
            }
            .attributes(clientRegistrationId("syfosmregister-m2m"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("Nav-CallId", sykmeldingId)
            .retrieve()
            .onStatus({ it.isError }) { req, res -> onStatusError(res) }
            .body<SyfoSmRegisterSykmelding>()
            ?: throw RuntimeException("Body is not SykInnSykmeldingDTO")

    fun getSykmeldingByIdent(ident: String): List<SyfoSmRegisterSykmelding> =
        syfosmregisterClient
            .get()
            .uri { uriBuilder -> uriBuilder.path("/api/v2/sykmelding/sykinn/ident").build() }
            .attributes(clientRegistrationId("syfosmregister-m2m"))
            .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .header("X-IDENT", ident)
            .retrieve()
            .onStatus({ it.isError }) { req, res -> onStatusError(res) }
            .body<List<SyfoSmRegisterSykmelding>>()
            ?: throw RuntimeException("Body is not List<SykInnSykmeldingDTO>")

    private fun onStatusError(res: ClientHttpResponse) {
        throw RuntimeException("Error from syfosmregister got statuscode: ${res.statusCode}").also {
            logger.error(it.message, it)
        }
    }
}
