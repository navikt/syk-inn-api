package no.nav.tsm.syk_inn_api.person.pdl

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.junit5.StartStop
import no.nav.tsm.syk_inn_api.security.TexasClient
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.web.reactive.function.client.WebClient

@ExtendWith(MockKExtension::class)
class PdlClientTest {

    @StartStop val mockWebServer: MockWebServer = MockWebServer()

    private lateinit var client: PdlClient
    private lateinit var texasClient: TexasClient

    private val token = "mocked-token"

    @BeforeEach
    fun setup() {
        texasClient = mockk()
        client =
            PdlClient(
                webClientBuilder = WebClient.builder(),
                pdlEndpointUrl = mockWebServer.url("/").toString(),
                texasClient = texasClient,
            )
    }

    @Test
    fun `should retrieve birth date and return success`() {
        every { texasClient.requestToken("tsm", "tsm-pdl-cache") } returns
            TexasClient.TokenResponse(
                access_token = token,
                expires_in = 1000,
                token_type = "Bearer",
            )

        @Language("JSON")
        val responseJson =
            """
                |{
                |  "navn": {
                |    "fornavn": "Test",
                |    "mellomnavn": null,
                |    "etternavn": "Person"
                |  },
                |  "foedselsdato": "2000-01-01",
                |  "identer": []
                |}"""
                .trimMargin()

        val response =
            MockResponse.Builder()
                .setHeader("Content-Type", "application/json")
                .body(responseJson)
                .build()

        mockWebServer.enqueue(response)

        val result = client.getPerson("01010078901")

        assertTrue(result.isSuccess)
        assertEquals(LocalDate.of(2000, 1, 1), result.getOrThrow().foedselsdato)
    }

    @Test
    fun `should return failure when unauthorized`() {
        every { texasClient.requestToken("tsm", "tsm-pdl-cache") } returns
            TexasClient.TokenResponse(
                access_token = "invalid-token",
                expires_in = 1000,
                token_type = "Bearer",
            )

        val response =
            MockResponse.Builder().setHeader("Content-Type", "application/json").code(401).build()
        mockWebServer.enqueue(response)

        val result = client.getPerson("01010078901")

        assertTrue(result.isFailure)
    }

    @Test
    fun `should return failure when fnr is missing or invalid`() {
        every { texasClient.requestToken("tsm", "tsm-pdl-cache") } returns
            TexasClient.TokenResponse(
                access_token = token,
                expires_in = 1000,
                token_type = "Bearer",
            )

        val response =
            MockResponse.Builder().setHeader("Content-Type", "application/json").code(400).build()
        mockWebServer.enqueue(response)

        val result = client.getPerson("")

        assertTrue(result.isFailure)
    }
}
