package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.fail
import kotlinx.coroutines.test.runTest
import no.nav.tsm.utils.simpleUnitTestEnvironment
import no.nav.tsm.utils.testJsonObjectMapper

class HprCloudClientTest {

    @Test
    fun `should return sykmelder based on hpr number`() = runTest {
        val hprNummer = "12345"
        val mockEngine = MockEngine { request ->
            assertEquals(
                simpleUnitTestEnvironment.external().helsenettproxy +
                    "/api/v2/behandlerMedHprNummer",
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
                httpClient = HttpClient(mockEngine) { install(ContentNegotiation) { jackson() } },
                texasClient = mockk(relaxed = true),
                environment = simpleUnitTestEnvironment,
            )

        val response = hprClient.getSykmelderByHpr(hprNummer).getOrNull()
        assertNotNull(response)
        assertEquals("12345678901", response.ident)
        assertEquals(hprNummer, response.hprNummer)
    }

    @Test
    fun `should return null when sykmelder is not found`() = runTest {
        val hprNummer = "13378010"
        val mockEngine = MockEngine { request ->
            assertEquals(
                simpleUnitTestEnvironment.external().helsenettproxy +
                    "/api/v2/behandlerMedHprNummer",
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
                environment = simpleUnitTestEnvironment,
            )

        val response = hprClient.getSykmelderByHpr(hprNummer)

        response.fold({ assertEquals(HprClient.HprErrors.NotFound, it) }) {
            fail("Should not be right")
        }
    }
}
