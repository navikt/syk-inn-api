package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler

import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson3.jackson
import io.mockk.mockk
import kotlin.collections.emptyList
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import no.nav.tsm.utils.simpleUnitTestEnvironment
import no.nav.tsm.utils.testJsonObjectMapper

class BehandlerClientTest {

    @Test
    fun `should return sykmelder based on hpr number`() = runTest {
        val hprNummer = "12345"
        val mockEngine = MockEngine { request ->
            request.url.toString() shouldEqual
                simpleUnitTestEnvironment.external().tsmBehandler + "/api/behandler/search"

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content =
                    testJsonObjectMapper.writeValueAsString(
                        TsmBehandler(
                            ident = "12345678901",
                            hpr = hprNummer,
                            navn = BehandlerNavn("fornavn", null, "mellomnavn"),
                            godkjenning = emptyList(),
                            suspendert = false,
                        )
                    ),
            )
        }

        val tsmBehandlerClient =
            TsmBehandlerCloudClient(
                httpClient = HttpClient(mockEngine) {},
                texasClient = mockk(relaxed = true),
                environment = simpleUnitTestEnvironment,
            )

        val response = tsmBehandlerClient.getSykmelderByHpr(hprNummer).getOrNull()
        response.shouldNotBeNull()
        response.ident shouldEqual "12345678901"
        response.hprNummer shouldEqual hprNummer
    }

    @Test
    fun `should return NotFound when sykmelder is not found`() = runTest {
        val hprNummer = "13378010"
        val mockEngine = MockEngine { request ->
            request.url.toString() shouldEqual
                simpleUnitTestEnvironment.external().tsmBehandler + "/api/behandler/search"

            respond(status = HttpStatusCode.NotFound, content = "")
        }

        val tsmBehandlerClient =
            TsmBehandlerCloudClient(
                httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { jackson() } },
                texasClient = mockk(relaxed = true),
                environment = simpleUnitTestEnvironment,
            )

        tsmBehandlerClient.getSykmelderByHpr(hprNummer).fold({
            it shouldEqual TsmBehandlerClient.BehandlerErrors.NotFound
        }) {
            fail("Should not be right")
        }
    }
}
