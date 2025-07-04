package no.nav.tsm.syk_inn_api.sykmelder.hpr

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import no.nav.tsm.syk_inn_api.security.TexasClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient

@ExtendWith(MockKExtension::class)
class HelsenettProxyTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: HelsenettProxyClient
    private lateinit var texasClient: TexasClient

    private val token = "mocked-token"

    val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()

        texasClient = mockk()

        client =
            HelsenettProxyClient(
                webClientBuilder = WebClient.builder(),
                baseUrl = baseUrl,
                texasClient = texasClient
            )
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `should send correct request and return success`() {
        every { texasClient.requestToken("teamsykmelding", "syfohelsenettproxy") } returns
            TexasClient.TokenResponse("mocked-token", expires_in = 1000, token_type = "Bearer")

        val sykmelder = getSykmelderTestData()

        val response =
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody(objectMapper.writeValueAsString(sykmelder))
                .setResponseCode(200)
        mockWebServer.enqueue(response)

        val result = client.getSykmelderByHpr("123456", "sykmeldingId")
        val request = mockWebServer.takeRequest()
        assertEquals("/api/v2/behandlerMedHprNummer", request.path)
        assertEquals("Bearer $token", request.getHeader("Authorization"))
        assertEquals("application/json", request.getHeader("Content-Type"))
        assertEquals("sykmeldingId", request.getHeader("Nav-CallId"))
        assertEquals("123456", request.getHeader("hprNummer"))

        assertTrue(result.isSuccess)
    }

    @Test
    fun `should return failure when unauthorized`() {
        every { texasClient.requestToken("teamsykmelding", "syfohelsenettproxy") } returns
            TexasClient.TokenResponse("invalid-token", expires_in = 1000, token_type = "Bearer")

        val response = MockResponse().setResponseCode(401).setBody("Unauthorized")

        mockWebServer.enqueue(response)

        val result = client.getSykmelderByHpr("123456", "sykmeldingId")

        assertTrue(result.isFailure)
    }

    @Test
    fun `should return failure when hprnummer header is missing or invalid`() {
        every { texasClient.requestToken("teamsykmelding", "syfohelsenettproxy") } returns
            TexasClient.TokenResponse("mocked-token", expires_in = 1000, token_type = "Bearer")

        val response =
            MockResponse()
                .setResponseCode(400)
                .setBody("Bad request: missing or invalid hprNummer header")

        mockWebServer.enqueue(response)

        val result = client.getSykmelderByHpr("INVALID", "sykmeldingId")

        assertTrue(result.isFailure)
    }

    fun getSykmelderTestData(): HprSykmelder {
        return HprSykmelder(
            godkjenninger =
                listOf(
                    HprGodkjenning(
                        helsepersonellkategori = HprKode(aktiv = true, oid = 0, verdi = "LE"),
                        autorisasjon = HprKode(aktiv = true, oid = 7704, verdi = "1"),
                        tillegskompetanse = null
                    )
                ),
            fnr = "09099012345",
            hprNummer = "123456789",
            fornavn = "James",
            mellomnavn = "007",
            etternavn = "Bond"
        )
    }
}
