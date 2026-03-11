package modules.sykmeldinger.sykmelder.clients

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import core.Environment
import core.ExternalApi
import core.Runtime
import core.RuntimeEnvironments
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import modules.sykmeldinger.sykmelder.clients.hpr.HprCloudClient
import modules.sykmeldinger.sykmelder.clients.hpr.HprSykmelder

class HprClientTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should return sykmelder based on hpr number`() = runTest {
        val hprNummer = "12345"
        val mockEngine = MockEngine { request ->
            assertEquals(
                testEnv.external().helsenettproxy + "/api/v2/behandlerMedHprNummer",
                request.url.toString(),
            )
            assertEquals(
                hprNummer,
                request.headers["HprNummer"],
                "HprNummer header should match the behandlerHpr parameter",
            )

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content =
                    objectMapper.writeValueAsString(
                        HprSykmelder(
                            fnr = "12345678901",
                            hprNummer = hprNummer,
                            fornavn = "Cooper",
                            mellomnavn = null,
                            etternavn = "Howard",
                            godkjenninger = emptyList(),
                        )
                    ),
            )
        }

        val hprClient =
            HprCloudClient(
                httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { jackson() } },
                texasClient = mockk(relaxed = true),
                environment = testEnv,
            )

        val response = hprClient.getSykmelderByHpr(hprNummer)
        assertNotNull(response)
        assertEquals("12345678901", response.ident)
        assertEquals(hprNummer, response.hprNummer)
        assertEquals("Cooper", response.fornavn)
        assertEquals("Howard", response.etternavn)
    }

    @Test
    fun `should return null when sykmelder is not found`() = runTest {
        val hprNummer = "13378010"
        val mockEngine = MockEngine { request ->
            assertEquals(
                testEnv.external().helsenettproxy + "/api/v2/behandlerMedHprNummer",
                request.url.toString(),
            )
            assertEquals(
                hprNummer,
                request.headers["HprNummer"],
                "HprNummer header should match the behandlerHpr parameter",
            )

            respond(status = HttpStatusCode.NotFound, content = "")
        }

        val hprClient =
            HprCloudClient(
                httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { jackson() } },
                texasClient = mockk(relaxed = true),
                environment = testEnv,
            )

        val response = hprClient.getSykmelderByHpr(hprNummer)
        assertNull(response)
    }
}

private val testEnv =
    Environment(
        runtime = Runtime(env = RuntimeEnvironments.PROD, name = "test-app"),
        texas = mockk(relaxed = true),
        kafka = mockk(relaxed = true),
        postgres = mockk(relaxed = true),
        external = {
            ExternalApi(
                btsys = "https://test.btsys.endpoint",
                tsmPdlCache = "https://test.pdlcache.endpoint",
                helsenettproxy = "https://test.helsenettproxy.endpoint",
            )
        },
        auth = mockk(relaxed = true),
    )
