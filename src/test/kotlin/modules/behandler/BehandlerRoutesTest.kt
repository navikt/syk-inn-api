package modules.behandler

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.plugins.di.dependencies
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import modules.sykmeldinger.SykmeldingerService
import modules.sykmeldinger.sykmelder.configureSykmelderDependencies
import no.nav.tsm.configureTestStuff
import utils.configureMockedEnvironment

class BehandlerRoutesTest {

    @Test
    fun testen() = testApplication {
        client = createClient { install(ContentNegotiation) { jackson() } }

        val mocken = mockk<SykmeldingerService>()
        application {
            dependencies.provide<SykmeldingerService> { mocken }

            configureMockedEnvironment()
            configureSykmelderDependencies()
            configureTestStuff()
        }

        every { mocken.test() } returns emptyList()

        val response = client.get("/test")
        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.body<List<Pair<String, String>>>()
        assertEquals(emptyList(), body)
    }
}
