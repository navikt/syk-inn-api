package no.nav.tsm.sykinnapi.client.syfohelsenettproxy

import no.nav.security.mock.oauth2.http.objectMapper
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tsm.sykinnapi.config.token.M2MTokenService
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers
import org.mockito.Mockito
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.web.reactive.function.client.WebClient
import kotlin.test.assertEquals

@EnableJwtTokenValidation
@EnableMockOAuth2Server
@SpringBootTest
class SyfohelsenettproxyClientTest {

    private val syfohelsenettproxyMockWebServer: MockWebServer =
        MockWebServer()
            .also { it.start() }
            .also { System.setProperty("syfohelsenettproxy.url", "http://localhost:${it.port}") }

    @MockitoBean
    private lateinit var m2mTokenService: M2MTokenService

    private lateinit var syfohelsenettproxyClient: SyfohelsenettproxyClient

    @BeforeEach
    fun setup() {
        Mockito.`when`(m2mTokenService.getM2MToken(ArgumentMatchers.anyString())).thenReturn("token")

        syfohelsenettproxyClient =
            SyfohelsenettproxyClient(
                syfohelsenettproxyM2mWebBuilder = WebClient.builder(),
                syfohelsenettproxyBaseUrl = System.getProperty("syfohelsenettproxy.url"),
                m2mTokenService = m2mTokenService,
            )
    }

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
