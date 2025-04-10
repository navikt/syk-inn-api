package no.nav.tsm.syk_inn_api.client

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import no.nav.tsm.syk_inn_api.exception.BtsysException
import no.nav.tsm.syk_inn_api.service.TokenService
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@ExtendWith(MockKExtension::class)
class BtsysProxyClientTest {

    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: BtsysProxyClient

    private val token = "mocked-token"

    private lateinit var tokenService: TokenService

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()

        tokenService = mockk()

        client = BtsysProxyClient(
            webClientBuilder = WebClient.builder(),
            btsysEndpointUrl = baseUrl,
            tokenService = tokenService
        )
    }

    @AfterEach
    fun teardown() {
        mockWebServer.shutdown()
    }

    @Test
    fun `should send correct request and return success`() {

        every { tokenService.getTokenForBtsys() } returns TexasClient.TokenResponse(
            "mocked-token",
            expiresIn = 1000,
            tokenType = "Bearer"
        )

        val response = MockResponse()
            .setHeader("Content-Type", "application/json")
            .setBody("true")
            .setResponseCode(200)
        mockWebServer.enqueue(response)

        val result = client.checkSuspensionStatus("12345678901", "2025-04-10")
        val request = mockWebServer.takeRequest()

        assertEquals("/api/v1/suspensjon/status?oppslagsdato=2025-04-10", request.path)
        assertEquals("Bearer $token", request.getHeader("Authorization"))
        assertEquals("syk-inn-api", request.getHeader("Nav-Consumer-Id"))
        assertEquals("12345678901", request.getHeader("Nav-Personident"))

        assertTrue(result is Result.Success)
        assertEquals(true, result.data)
    }

    @Test
    fun `should return failure when unauthorized`() {
        every { tokenService.getTokenForBtsys() } returns TexasClient.TokenResponse(
            "invalid-token",
            expiresIn = 1000,
            tokenType = "Bearer"
        )

        val response = MockResponse()
            .setResponseCode(401)
            .setBody("Unauthorized")

        mockWebServer.enqueue(response)

        val result = client.checkSuspensionStatus("12345678901", "2025-04-10")

        assertTrue(result is Result.Failure)
    }


    @Test
    fun `should return failure when personident header is missing or invalid`() {
        every { tokenService.getTokenForBtsys() } returns TexasClient.TokenResponse(
            "mocked-token",
            expiresIn = 1000,
            tokenType = "Bearer"
        )

        val response = MockResponse()
            .setResponseCode(400)
            .setBody("Bad request: Missing or invalid Nav-Personident header")

        mockWebServer.enqueue(response)

        val result = client.checkSuspensionStatus("INVALID", "2025-04-10")

        assertTrue(result is Result.Failure)
        assertTrue(result.error is BtsysException)
    }

}
