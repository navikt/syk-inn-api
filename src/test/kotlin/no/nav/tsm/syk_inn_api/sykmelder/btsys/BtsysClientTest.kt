package no.nav.tsm.syk_inn_api.sykmelder.btsys

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail
import no.nav.tsm.syk_inn_api.security.TexasClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient

@ExtendWith(MockKExtension::class)
class BtsysClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: BtsysClient

    private val token = "mocked-token"

    private lateinit var texasClient: TexasClient

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()

        texasClient = mockk()

        client =
            BtsysClient(
                webClientBuilder = WebClient.builder(),
                btsysEndpointUrl = baseUrl,
                texasClient = texasClient,
            )
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `should send correct request and return success`() {

        every { texasClient.requestToken("team-rocket", "btsys-api") } returns
            TexasClient.TokenResponse(
                access_token = token,
                expires_in = 1000,
                token_type = "Bearer",
            )

        val response =
            MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""{ "suspendert": false }""")
                .setResponseCode(200)
        mockWebServer.enqueue(response)

        val result = client.checkSuspensionStatus("12345678901", "2025-04-10")
        val request = mockWebServer.takeRequest()

        assertEquals("/api/v1/suspensjon/status?oppslagsdato=2025-04-10", request.path)
        assertEquals("Bearer $token", request.getHeader("Authorization"))
        assertEquals("syk-inn-api", request.getHeader("Nav-Consumer-Id"))
        assertEquals("12345678901", request.getHeader("Nav-Personident"))

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

        val response = MockResponse().setResponseCode(401).setBody("Unauthorized")

        mockWebServer.enqueue(response)

        val result = client.checkSuspensionStatus("12345678901", "2025-04-10")

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

        val response =
            MockResponse()
                .setResponseCode(400)
                .setBody("Bad request: Missing or invalid Nav-Personident header")

        mockWebServer.enqueue(response)

        val result = client.checkSuspensionStatus("INVALID", "2025-04-10")

        assertTrue(result.isFailure)
        assertThrows<IllegalStateException>("Missing or invalid Nav-Personident header") {
            result.getOrThrow()
        }
    }
}
