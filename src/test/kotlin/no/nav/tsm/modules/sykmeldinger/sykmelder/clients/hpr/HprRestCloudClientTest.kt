package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import no.nav.tsm.utils.simpleUnitTestEnvironment

class HprRestCloudClientTest {

    @Test
    fun `should return sykmelder based on hpr number`() = runTest {
        val hpr = "69420"

        val mockEngine = MockEngine { request ->
            request.url.toString() shouldEqual "https://test.hpr.endpoint/v1/personerutvidet/69420"

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content =
                    """{ "hprNummer": 69420, "person": { "nin": "12345678901", "fornavn": "Liten", "etternavn": "Tester" }}""",
            )
        }

        val client =
            HprRestCloudClient(
                httpClient = HttpClient(mockEngine) {},
                texasClient = mockk(relaxed = true),
                environment = simpleUnitTestEnvironment,
            )

        val response = client.getSykmelderByHpr(hpr).getOrNull()
        response.shouldNotBeNull()
        response.ident shouldEqual "12345678901"
        response.hprNummer shouldEqual hpr
    }

    @Test
    fun `should return sykmelder based on ident (fnr)`() = runTest {
        val ident = "12345678901"

        val mockEngine = MockEngine { request ->
            request.url.toString() shouldEqual "https://test.hpr.endpoint/v1/personerutvidet"

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content =
                    """{ "hprNummer": 69420, "person": { "nin": "12345678901", "fornavn": "Liten", "etternavn": "Tester" }}""",
            )
        }

        val client =
            HprRestCloudClient(
                httpClient = HttpClient(mockEngine) {},
                texasClient = mockk(relaxed = true),
                environment = simpleUnitTestEnvironment,
            )

        val response = client.getSykmelderByIdent(ident).getOrNull()
        response.shouldNotBeNull()
        response.ident shouldEqual ident
        response.hprNummer shouldEqual "69420"
    }
}
