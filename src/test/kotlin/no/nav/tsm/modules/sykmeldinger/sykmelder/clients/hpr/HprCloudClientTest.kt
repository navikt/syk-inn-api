package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import no.nav.tsm.utils.simpleUnitTestEnvironment
import no.nav.tsm.utils.testJsonObjectMapper

class HprCloudClientTest {

    @Test
    fun `should return sykmelder based on hpr number`() = runTest {
        val hprNummer = "12345"
        val mockEngine = MockEngine { request ->
            request.url.toString() shouldEqual
                simpleUnitTestEnvironment.external().helsenettproxy +
                    "/api/v2/behandlerMedHprNummer"
            request.headers["HprNummer"] shouldEqual hprNummer

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content =
                    testJsonObjectMapper.writeValueAsString(
                        HprSykmelder(
                            fnr = "12345678901",
                            hprNummer = hprNummer,
                            fornavn = "Test",
                            mellomnavn = null,
                            etternavn = "Testessen",
                            godkjenninger = emptyList(),
                        )
                    ),
            )
        }

        val hprClient =
            HprCloudClient(
                httpClient = HttpClient(mockEngine) {},
                texasClient = mockk(relaxed = true),
                environment = simpleUnitTestEnvironment,
            )

        val response = hprClient.getSykmelderByHpr(hprNummer).getOrNull()
        response.shouldNotBeNull()
        response.ident shouldEqual "12345678901"
        response.hprNummer shouldEqual hprNummer
    }

    @Test
    fun `should return NotFound when sykmelder is not found`() = runTest {
        val hprNummer = "13378010"
        val mockEngine = MockEngine { request ->
            request.url.toString() shouldEqual
                simpleUnitTestEnvironment.external().helsenettproxy +
                    "/api/v2/behandlerMedHprNummer"
            request.headers["HprNummer"] shouldEqual hprNummer

            respond(status = HttpStatusCode.NotFound, content = "")
        }

        val hprClient =
            HprCloudClient(
                httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { jackson() } },
                texasClient = mockk(relaxed = true),
                environment = simpleUnitTestEnvironment,
            )

        hprClient.getSykmelderByHpr(hprNummer).fold({
            it shouldEqual HprClient.HprErrors.NotFound
        }) {
            fail("Should not be right")
        }
    }
}
