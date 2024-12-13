package no.nav.tsm.sykinnapi.client.syfohelsenettproxy

import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpMethod.GET
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.test.web.client.MockRestServiceServer
import org.springframework.test.web.client.match.MockRestRequestMatchers.method
import org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo
import org.springframework.test.web.client.response.MockRestResponseCreators.withStatus
import org.springframework.web.client.RestClient
import java.net.URI
import kotlin.test.assertEquals

@RestClientTest(SyfohelsenettproxyClient::class)
class SyfohelsenettproxyClientTest(
    @Value("\${syfohelsenettproxy.url}") private val syfohelsenettproxyBaseUrl: String
) {

    @TestConfiguration
    class TestConfig(@Value("\${syfohelsenettproxy.url}") val syfohelsenettproxyBaseUrl: String) {
        @Bean("syfohelsenettproxyClient")
        fun syfohelsenettproxyClient(builder: RestClient.Builder): SyfohelsenettproxyClient {
            return SyfohelsenettproxyClient(builder.baseUrl(syfohelsenettproxyBaseUrl).build())
        }
    }

    @Autowired private lateinit var mockRestServiceServer: MockRestServiceServer

    @Autowired private lateinit var syfohelsenettproxyClient: SyfohelsenettproxyClient

    @Autowired private lateinit var objectMapper: ObjectMapper

    @Test
    internal fun `Should return behandler`() {
        val behandlerFnr = "23123131"
        val behandlerHpr = "123123"
        val sykmeldingId = "21322-223-21333-22"
        val response =
            objectMapper.writeValueAsString(
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
            .expect(requestTo(URI("$syfohelsenettproxyBaseUrl/api/v2/behandlerMedHprNummer")))
            .andExpect(method(GET))
            .andRespond(withStatus(OK).contentType(APPLICATION_JSON).body(response))

        val behandler = syfohelsenettproxyClient.getBehandlerByHpr(behandlerHpr, sykmeldingId)

        assertEquals(behandlerFnr, behandler.fnr)
    }
}
