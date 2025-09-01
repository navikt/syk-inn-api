package no.nav.tsm.syk_inn_api.sykmelder.hpr

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import no.nav.tsm.syk_inn_api.security.TexasClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.client.RestClient

@ExtendWith(MockKExtension::class)
class HelsenettProxyTest {

    @StartStop val mockWebServer: MockWebServer = MockWebServer()

    private lateinit var client: HelsenettProxyClient
    private lateinit var texasClient: TexasClient

    private val token = "mocked-token"

    val objectMapper = jacksonObjectMapper()

    @BeforeEach
    fun setup() {
        texasClient = mockk()
        client =
            HelsenettProxyClient(
                restClientBuilder = RestClient.builder(),
                texasClient = texasClient,
                baseUrl = mockWebServer.url("/").toString(),
            )
    }

    @Test
    fun `should send correct request and return success`() {
        every { texasClient.requestToken("teamsykmelding", "syfohelsenettproxy") } returns
            TexasClient.TokenResponse("mocked-token", expires_in = 1000, token_type = "Bearer")

        val sykmelder = getSykmelderTestData()

        mockWebServer.enqueue(
            MockResponse.Builder()
                .setHeader("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(sykmelder))
                .code(200)
                .build(),
        )

        val result = client.getSykmelderByHpr("123456", "sykmeldingId")
        val request = mockWebServer.takeRequest()
        assertEquals("/api/v2/behandlerMedHprNummer", request.url.encodedPath)
        assertEquals("Bearer $token", request.headers["Authorization"])
        assertEquals("application/json", request.headers["Content-Type"])
        assertEquals("sykmeldingId", request.headers["Nav-CallId"])
        assertEquals("123456", request.headers["hprNummer"])

        assertTrue(result.isSuccess)
    }

    @Test
    fun `should return failure when unauthorized`() {
        every { texasClient.requestToken("teamsykmelding", "syfohelsenettproxy") } returns
            TexasClient.TokenResponse("invalid-token", expires_in = 1000, token_type = "Bearer")

        mockWebServer.enqueue(MockResponse.Builder().code(401).body("Unauthorized").build())

        val result = client.getSykmelderByHpr("123456", "sykmeldingId")

        assertTrue(result.isFailure)
    }

    @Test
    fun `should return failure when hprnummer header is missing or invalid`() {
        every { texasClient.requestToken("teamsykmelding", "syfohelsenettproxy") } returns
            TexasClient.TokenResponse("mocked-token", expires_in = 1000, token_type = "Bearer")

        mockWebServer.enqueue(
            MockResponse.Builder()
                .code(400)
                .body("Bad request: missing or invalid hprNummer header")
                .build()
        )

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
                        tillegskompetanse = null,
                    ),
                ),
            fnr = "09099012345",
            hprNummer = "123456789",
            fornavn = "James",
            mellomnavn = "007",
            etternavn = "Bond",
        )
    }
}
