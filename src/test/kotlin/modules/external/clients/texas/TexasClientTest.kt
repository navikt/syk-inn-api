package modules.external.clients.texas

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteReadChannel
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.core.Environment
import no.nav.tsm.core.RuntimeEnvironments
import no.nav.tsm.core.Texas
import utils.parse

class TexasClientTest {

    @Test
    fun `should exchange token for correct target`() = testApplication {
        val mockEngine = MockEngine { request ->
            assertEquals(request.url.toString(), testEnv.texas().tokenEndpoint)

            val payload = request.body.toByteArray().parse<TexasClient.TokenRequest>()
            assertEquals("api://prod-gcp.tsm.tsm-pdl-cache/.default", payload.target)

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content =
                    ByteReadChannel(
                        """{"access_token":"ay.aeuheu","expires_in":3600,"token_type":"Bearer"}"""
                    ),
            )
        }

        val texas =
            TexasClient(
                env = testEnv,
                httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { jackson() } },
            )

        val response = texas.requestToken("tsm", "tsm-pdl-cache")
        assertEquals("ay.aeuheu", response.token)
    }
}

private val testEnv =
    Environment(
        runtimeEnv = RuntimeEnvironments.PROD,
        texas = { Texas(tokenEndpoint = "https://test.token.endpoint") },
        kafka = mockk(relaxed = true),
        postgres = mockk(relaxed = true),
        external = mockk(relaxed = true),
    )
