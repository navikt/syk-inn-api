package no.nav.tsm.syk_inn_api.client

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import no.nav.tsm.syk_inn_api.service.TokenService
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient

@ExtendWith(MockKExtension::class)
class PdlClientTest {
    private lateinit var mockWebServer: MockWebServer
    private lateinit var client: PdlClient

    private val token = "mocked-token"

    private lateinit var tokenService: TokenService

    @BeforeEach
    fun setup() {
        mockWebServer = MockWebServer()
        mockWebServer.start()

        val baseUrl = mockWebServer.url("/").toString()

        tokenService = mockk()

        client =
            PdlClient(
                webClientBuilder = WebClient.builder(),
                pdlEndpointUrl = baseUrl,
                tokenService = tokenService,
            )
    }

    @Test
    fun `should retrieve birth date and return success`() {
        every { tokenService.getTokenForPdl() } returns
            TexasClient.TokenResponse(
                accessToken = token,
                expiresIn = 1000,
                tokenType = "Bearer",
            )

        val responseJson =
            """
        {
          "navn": {
            "fornavn": "Test",
            "mellomnavn": null,
            "etternavn": "Person"
          },
          "foedselsdato": "2000-01-01",
          "identer": []
        }
    """
                .trimIndent()

        val response =
            MockResponse().setHeader("Content-Type", "application/json").setBody(responseJson)
        mockWebServer.enqueue(response)

        val result = client.getFodselsdato("01010078901")

        assertTrue(result is Result.Success)
        assertEquals(LocalDate.of(2000, 1, 1), result.data.foedselsdato)
    }

    @Test
    fun `should return failure when unauthorized`() {
        every { tokenService.getTokenForPdl() } returns
            TexasClient.TokenResponse(
                accessToken = "invalid-token",
                expiresIn = 1000,
                tokenType = "Bearer",
            )

        val response =
            MockResponse().setHeader("Content-Type", "application/json").setResponseCode(401)
        mockWebServer.enqueue(response)

        val result = client.getFodselsdato("01010078901")

        assertTrue(result is Result.Failure)
    }

    @Test
    fun `should return failure when fnr is missing or invalid`() {
        every { tokenService.getTokenForPdl() } returns
            TexasClient.TokenResponse(
                accessToken = token,
                expiresIn = 1000,
                tokenType = "Bearer",
            )

        val response =
            MockResponse().setHeader("Content-Type", "application/json").setResponseCode(400)
        mockWebServer.enqueue(response)

        val result = client.getFodselsdato("")

        assertTrue(result is Result.Failure)
    }
}
