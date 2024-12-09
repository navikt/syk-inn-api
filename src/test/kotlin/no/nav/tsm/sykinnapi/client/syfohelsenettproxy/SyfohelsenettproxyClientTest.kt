package no.nav.tsm.sykinnapi.client.syfohelsenettproxy

import com.fasterxml.jackson.databind.ObjectMapper
import kotlin.test.assertEquals
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.test.web.client.MockRestServiceServer

@RestClientTest(SyfohelsenettproxyClient::class)
class SyfohelsenettproxyClientTest {

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

        mockRestServiceServer.expect()
        /*
        mockRestServiceServer.expect() . .enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(response))
        )*/

        val behandler = syfohelsenettproxyClient.getBehandlerByHpr(behandlerHpr, sykmeldingId)

        assertEquals(behandlerFnr, behandler.fnr)
    }
}
