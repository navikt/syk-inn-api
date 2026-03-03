package modules.sykmeldinger

import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.plugins.di.*
import io.ktor.server.testing.*
import io.mockk.every
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.modules.sykmeldinger.SykmeldingService
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingRoutes

class SykmeldingRoutesTest {

    @Test
    fun testen() = testApplication {
        val mocken = mockk<SykmeldingService>()

        application {
            dependencies.provide<SykmeldingService> { mocken }

            configureSykmeldingRoutes()
        }

        every { mocken.test() } returns Unit

        val response = client.get("/test")

        assertEquals(HttpStatusCode.Created, response.status)
    }
}
