package no.nav.tsm.modules.sykmeldinger.pdl

import arrow.core.getOrElse
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteReadChannel
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import no.nav.tsm.utils.simpleUnitTestEnvironment
import no.nav.tsm.utils.testJsonObjectMapper

class PdlCloudClientTest {

    val goodResponseBodyJson =
        testJsonObjectMapper.writeValueAsString(
            PdlPerson(
                navn =
                    PdlNavn(
                        fornavn = "Fornavn",
                        mellomnavn = "Mellomnavn",
                        etternavn = "Etternavn",
                    ),
                foedselsdato = LocalDate.now().minusYears(35),
                identer =
                    listOf(
                        Ident(
                            ident = "12345678910",
                            gruppe = Identgruppe.FOLKEREGISTERIDENT,
                            historisk = false,
                        )
                    ),
            )
        )

    @Test
    fun `should properly deserialize response`() = testApplication {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/person", request.url.fullPath)

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content = ByteReadChannel(goodResponseBodyJson),
            )
        }

        val pdlClient =
            PdlCloudClient(
                httpClient = HttpClient(mockEngine) {},
                texasClient = mockk(relaxed = true),
                environment = simpleUnitTestEnvironment,
            )

        val response =
            pdlClient.getPerson("hello").getOrElse { fail("Failed to get person from PDL") }

        response.foedselsdato shouldBe LocalDate.now().minusYears(35)
    }
}
