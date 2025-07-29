package no.nav.tsm.syk_inn_api.sykmelder.btsys

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import no.nav.tsm.syk_inn_api.security.TexasClient
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient

@ExtendWith(MockKExtension::class)
class BtsysClientTest {

    @StartStop val mockWebServer: MockWebServer = MockWebServer()

    private lateinit var client: BtsysClient

    private val token = "mocked-token"

    private lateinit var texasClient: TexasClient

    @BeforeEach
    fun setup() {
        texasClient = mockk()
        client =
            BtsysClient(
                webClientBuilder = WebClient.builder(),
                btsysEndpointUrl = mockWebServer.url("/").toString(),
                texasClient = texasClient,
            )
    }

    @Test
    fun `should send correct request and return success`() {

        every { texasClient.requestToken("team-rocket", "btsys-api") } returns
            TexasClient.TokenResponse(
                access_token = token,
                expires_in = 1000,
                token_type = "Bearer",
            )

        mockWebServer.enqueue(
            MockResponse.Builder()
                .setHeader("Content-Type", "application/json")
                .body("""{ "suspendert": false }""")
                .code(200)
                .build(),
        )

        val result = client.checkSuspensionStatus("12345678901", LocalDate.parse("2025-04-10"))
        val request = mockWebServer.takeRequest()

        assertEquals("/api/v1/suspensjon/status", request.url.encodedPath)
        assertEquals("oppslagsdato=2025-04-10", request.url.query)
        assertEquals("Bearer $token", request.headers["Authorization"])
        assertEquals("syk-inn-api", request.headers["Nav-Consumer-Id"])
        assertEquals("12345678901", request.headers["Nav-Personident"])

        result.fold({ assertFalse(it.suspendert) }) {
            fail("Expected success but got failure: $it")
        }
    }

    @Test
    fun `should return failure when unauthorized`() {
        every { texasClient.requestToken("team-rocket", "btsys-api") } returns
            TexasClient.TokenResponse(
                access_token = "invalid-token",
                expires_in = 1000,
                token_type = "Bearer",
            )

        mockWebServer.enqueue(MockResponse.Builder().code(401).body("Unauthorized").build())

        val result = client.checkSuspensionStatus("12345678901", LocalDate.parse("2025-04-10"))

        assertTrue(result.isFailure)
    }

    @Test
    fun `should return failure when personident header is missing or invalid`() {
        every { texasClient.requestToken("team-rocket", "btsys-api") } returns
            TexasClient.TokenResponse(
                access_token = token,
                expires_in = 1000,
                token_type = "Bearer",
            )

        mockWebServer.enqueue(
            MockResponse.Builder()
                .code(400)
                .body("Bad request: Missing or invalid Nav-Personident header")
                .build()
        )

        val result = client.checkSuspensionStatus("INVALID", LocalDate.parse("2025-04-10"))

        assertTrue(result.isFailure)
        assertThrows<IllegalStateException>("Missing or invalid Nav-Personident header") {
            result.getOrThrow()
        }
    }
}
