package modules.sykmeldinger

import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.jackson
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import modules.behandler.api.configureBehandlerRoutes
import no.nav.tsm.modules.sykmeldinger.SykmeldingService
import no.nav.tsm.plugins.configureSerialization

class SykmeldingRoutesTest {

    @Test
    fun testen() = testApplication {
        client = createClient { install(ContentNegotiation) { jackson() } }

        val mocken = mockk<SykmeldingService>()
        application {
            dependencies.provide<SykmeldingService> { mocken }

            configureSerialization()
            configureBehandlerRoutes()
        }

        every { mocken.test() } returns emptyList()

        val response = client.get("/test")
        assertEquals(HttpStatusCode.Created, response.status)

        val body = response.body<List<Pair<String, String>>>()
        assertEquals(emptyList(), body)
    }
}
