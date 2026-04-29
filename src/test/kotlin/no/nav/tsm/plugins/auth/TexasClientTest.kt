package no.nav.tsm.plugins.auth

import com.fasterxml.jackson.databind.PropertyNamingStrategies
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteReadChannel
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.utils.simpleUnitTestEnvironment

class TexasClientTest {

    val texasResponseMapper =
        jacksonObjectMapper().apply {
            setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
        }

    @Test
    fun `should exchange token for correct target`() = testApplication {
        val mockEngine = MockEngine { request ->
            assertEquals(request.url.toString(), simpleUnitTestEnvironment.texas().tokenEndpoint)

            val payload =
                texasResponseMapper.readValue<TexasClient.TokenRequest>(request.body.toByteArray())
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
            TexasClient(env = simpleUnitTestEnvironment, httpClient = HttpClient(mockEngine) {})

        val response = texas.requestToken("tsm", "tsm-pdl-cache")
        assertEquals("ay.aeuheu", response.token)
    }
}
