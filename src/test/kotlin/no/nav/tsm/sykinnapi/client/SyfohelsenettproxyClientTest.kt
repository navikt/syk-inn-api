package no.nav.tsm.sykinnapi.client

import kotlin.test.assertEquals
import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.springframework.web.reactive.function.client.WebClient

class SyfohelsenettproxyClientTest {

    private val syfohelsenettproxyMockWebServer: MockWebServer =
        MockWebServer()
            .also { it.start() }
            .also { System.setProperty("syfohelsenettproxy.url", "http://localhost:${it.port}") }

    private val syfohelsenettproxyClient: SyfohelsenettproxyClient =
        SyfohelsenettproxyClient(
            syfohelsenettproxyM2mWebBuilder =
                WebClient.builder().baseUrl(System.getProperty("syfohelsenettproxy.url")),
            syfohelsenettproxyBaseUrl = System.getProperty("syfohelsenettproxy.url"),
        )

    @AfterEach
    internal fun tearDown() {
        syfohelsenettproxyMockWebServer.shutdown()
    }

    @Test
    internal fun `Should return behandler`() {
        val behandlerFnr = "23123131"
        val behandlerHpr = "123123"
        val sykmeldingId = "21322-223-21333-22"

        val behandlerResponse =
            Behandler(
                godkjenninger = emptyList(),
                fnr = behandlerFnr,
                hprNummer = behandlerHpr,
                fornavn = "Fornavn",
                mellomnavn = null,
                etternavn = "etternavn",
            )

        syfohelsenettproxyMockWebServer.enqueue(
            MockResponse()
                .setResponseCode(200)
                .addHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(behandlerResponse))
        )

        val behandler = syfohelsenettproxyClient.getBehandlerByHpr(behandlerHpr, sykmeldingId)

        assertEquals(behandlerFnr, behandler.fnr)
    }
}
