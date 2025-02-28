package no.nav.tsm.sykinnapi.client.tsmpdl

import no.nav.tsm.sykinnapi.modell.tsmpdl.PdlPerson
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
class TsmPdlClient(
    @Qualifier("tsmPdlClientRestClient") private val tsmPdlClientRestClient: RestClient,
) {

    private val logger = LoggerFactory.getLogger(TsmPdlClient::class.java)

    fun getPdlPerson(ident: String) =
        tsmPdlClientRestClient
            .get()
            .uri { uriBuilder -> uriBuilder.path("/api/person").build() }
            .attributes(clientRegistrationId("tsmpdl-m2m"))
            .header(CONTENT_TYPE, APPLICATION_JSON_VALUE)
            .header("ident", ident)
            .retrieve()
            .onStatus({ it.isError }) { req, res -> onStatusError(res) }
            .body<PdlPerson>()
            ?: throw RuntimeException("Body is not PdlPerson")

    private fun onStatusError(res: ClientHttpResponse): Nothing {
        throw RuntimeException("Error from tsmpdl - got statuscode: ${res.statusCode}").also {
            logger.error(it.message, it)
        }
    }
}
