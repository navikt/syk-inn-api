package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys

import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.equals.shouldEqual
import io.ktor.client.engine.mock.*
import io.ktor.http.*
import io.ktor.utils.io.ByteReadChannel
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import no.nav.tsm.utils.client
import no.nav.tsm.utils.simpleUnitTestEnvironment
import no.nav.tsm.utils.testJsonObjectMapper

class BtsysCloudClientTest {

    @Test
    fun `should return true when sykmelder is suspendert`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals(
                simpleUnitTestEnvironment.external().btsys + "/api/v1/suspensjon/soek",
                request.url.toString(),
            )

            val payload =
                testJsonObjectMapper.readValue<Map<String, Any>>(request.body.toByteArray())
            payload["oppslagsdato"] shouldEqual LocalDate.now().toString()
            payload["personident"] shouldEqual "12345678910"

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
                environment = simpleUnitTestEnvironment,
            )

        val response = btsys.isSuspendert("12345678910", LocalDate.now()).getOrNull()
        assertEquals(true, response)
    }

    @Test
    fun `should return false when sykmelder is not suspendert`() = runTest {
        val mockEngine = MockEngine { request ->
            assertEquals(
                simpleUnitTestEnvironment.external().btsys + "/api/v1/suspensjon/soek",
                request.url.toString(),
            )

            val payload =
                testJsonObjectMapper.readValue<Map<String, Any>>(request.body.toByteArray())
            payload["oppslagsdato"] shouldEqual LocalDate.now().toString()
            payload["personident"] shouldEqual "12345678910"

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
                environment = simpleUnitTestEnvironment,
            )

        val response = btsys.isSuspendert("12345678910", LocalDate.now()).getOrNull()
        assertEquals(false, response)
    }
}
