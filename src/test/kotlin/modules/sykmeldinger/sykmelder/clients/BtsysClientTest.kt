package modules.sykmeldinger.sykmelder.clients

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import core.Environment
import core.ExternalApi
import core.Runtime
import core.RuntimeEnvironments
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import modules.sykmeldinger.sykmelder.clients.btsys.BtsysCloudClient
import modules.sykmeldinger.sykmelder.clients.btsys.BtsysResponse
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

class BtsysClientTest {
    private val objectMapper = jacksonObjectMapper()

    @Test
    fun `should return true when sykmelder is suspendert`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals(testEnv.external().btsys + "/api/v1/suspensjon/status", request.url.toString())

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content = objectMapper.writeValueAsString(BtsysResponse(suspendert = true)),
            )
        }

        val btsys = BtsysCloudClient(
            httpClient = io.ktor.client.HttpClient(mockEngine) { install(ContentNegotiation) { jackson() } },
            texasClient = mockk(relaxed = true),
            environment = testEnv,
        )

        val response = btsys.isSuspendert("12345678910", LocalDate.now())
        assertEquals(true, response)
    }

    @Test
    fun `should return false when sykmelder is not suspendert`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals(testEnv.external().btsys + "/api/v1/suspensjon/status", request.url.toString())

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content = objectMapper.writeValueAsString(BtsysResponse(suspendert = false)),
            )
        }

        val btsys = BtsysCloudClient(
            httpClient = io.ktor.client.HttpClient(mockEngine) { install(ContentNegotiation) { jackson() } },
            texasClient = mockk(relaxed = true),
            environment = testEnv,
        )

        val response = btsys.isSuspendert("12345678910", LocalDate.now())
        assertEquals(false, response)
    }
}

private val testEnv =
    Environment(
        runtime = Runtime(env = RuntimeEnvironments.PROD, name = "test-app"),
        texas = mockk(relaxed = true),
        kafka = mockk(relaxed = true),
        postgres = mockk(relaxed = true),
        external = { ExternalApi(btsys = "https://test.btsys.endpoint", tsmPdlCache = "https://test.pdlcache.endpoint", helsenettproxy = "https://test.helsenettproxy.endpoint") },
        auth = mockk(relaxed = true),
    )