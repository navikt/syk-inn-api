package no.nav.tsm.sykinnapi.client.syfohelsenettproxy

import com.fasterxml.jackson.databind.ObjectMapper
import java.net.URI
import kotlin.test.assertEquals
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpMethod.*
import org.springframework.http.HttpStatus.*
import org.springframework.http.MediaType.*
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.*
import org.springframework.test.web.client.response.MockRestResponseCreators.*

@RestClientTest(SyfohelsenettproxyClient::class)
class SyfohelsenettproxyClientTest(
    @Value("\${syfohelsenettproxy.url}") private val baseUrl: String
) {

    @Autowired private lateinit var mockRestServiceServer: MockRestServiceServer

    @Autowired private lateinit var syfohelsenettproxyClient: SyfohelsenettproxyClient

    @Autowired private lateinit var mapper: ObjectMapper

    @Test
    internal fun `Should return behandler`() {
        val behandlerFnr = "23123131"
        val behandlerHpr = "123123"
        val sykmeldingId = "21322-223-21333-22"
        val response =
            mapper.writeValueAsString(
                Behandler(
                    godkjenninger = emptyList(),
                    fnr = behandlerFnr,
                    hprNummer = behandlerHpr,
                    fornavn = "Fornavn",
                    mellomnavn = null,
                    etternavn = "etternavn",
                ),
            )
        mockRestServiceServer
            .expect(requestTo(URI("$baseUrl/api/v2/behandlerMedHprNummer")))
            .andExpect(method(GET))
            .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(response))

        val behandler = syfohelsenettproxyClient.getBehandlerByHpr(behandlerHpr, sykmeldingId)

        assertEquals(behandlerFnr, behandler.fnr)
    }
}
