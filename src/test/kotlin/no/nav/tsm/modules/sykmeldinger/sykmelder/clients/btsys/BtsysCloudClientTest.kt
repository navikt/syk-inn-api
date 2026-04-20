package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys

import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import no.nav.tsm.core.Environment
import no.nav.tsm.core.ExternalApi
import no.nav.tsm.core.Runtime
import no.nav.tsm.core.RuntimeEnvironments
import no.nav.tsm.utils.client

class BtsysCloudClientTest {

    @Test
    fun `should return true when sykmelder is suspendert`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals(
                testEnv.external().btsys + "/api/v1/suspensjon/status",
                request.url.toString(),
            )

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content = ByteReadChannel("""{"suspendert":true}"""),
            )
        }

        val btsys =
            BtsysCloudClient(
                httpClient = mockEngine.client(),
                texasClient = mockk(relaxed = true),
                environment = testEnv,
            )

        val response = btsys.isSuspendert("12345678910", LocalDate.now()).getOrNull()
        assertEquals(true, response)
    }

    @Test
    fun `should return false when sykmelder is not suspendert`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals(
                testEnv.external().btsys + "/api/v1/suspensjon/status",
                request.url.toString(),
            )

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content = ByteReadChannel("""{"suspendert":false}"""),
            )
        }

        val btsys =
            BtsysCloudClient(
                httpClient = mockEngine.client(),
                texasClient = mockk(relaxed = true),
                environment = testEnv,
            )

        val response = btsys.isSuspendert("12345678910", LocalDate.now()).getOrNull()
        assertEquals(false, response)
    }
}

private val testEnv =
    Environment(
        runtime = Runtime(env = RuntimeEnvironments.PROD, name = "test-app"),
        texas = mockk(relaxed = true),
        kafka = mockk(relaxed = true),
        postgres = mockk(relaxed = true),
        sykmeldingConfig = mockk(relaxed = true),
        external = {
            ExternalApi(
                btsys = "https://test.btsys.endpoint",
                tsmPdlCache = "https://test.pdlcache.endpoint",
                helsenettproxy = "https://test.helsenettproxy.endpoint",
            )
        },
        auth = mockk(relaxed = true),
    )
