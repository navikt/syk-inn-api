package modules.sykmeldinger.pdl

import core.Environment
import core.ExternalApi
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.http.fullPath
import io.ktor.serialization.jackson.jackson
import io.ktor.server.testing.testApplication
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import plugins.auth.TexasClient
import plugins.auth.TexasToken

class PdlCloudClientTest {

    @Test
    fun `dummy`() = testApplication {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/person", request.url.fullPath)

            println(request.url)

            respond("mordi")
        }

        val texasClient = mockk<TexasClient>()
        coEvery { texasClient.requestToken("tsm", "tsm-pdl-cache") } returns TexasToken("Tihi")

        val environment = mockk<Environment>()
        every { environment.external } returns
            {
                ExternalApi(
                    tsmPdlCache = "https://test.pdl.cache",
                    helsenettproxy = "https://test.helsenettproxy",
                    btsys = "https://test.btsys",
                )
            }

        val pdlClient =
            PdlCloudClient(
                httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { jackson() } },
                texasClient = texasClient,
                environment = environment,
            )

        // pdlClient.getPerson("hello")
    }
}
