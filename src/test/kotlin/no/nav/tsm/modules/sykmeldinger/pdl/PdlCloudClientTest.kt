package no.nav.tsm.modules.sykmeldinger.pdl

import arrow.core.getOrElse
import io.kotest.matchers.shouldBe
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.server.testing.testApplication
import io.ktor.utils.io.ByteReadChannel
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.fail
import no.nav.tsm.ktor.auth.texas.Texas
import no.nav.tsm.ktor.auth.texas.TexasToken
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
                        PdlIdent(
                            ident = "12345678910",
                            gruppe = PdlIdentgruppe.FOLKEREGISTERIDENT,
                            historisk = false,
                        )
                    ),
            )
        )

    val texasMock = mockk<Texas>()

    @BeforeTest
    fun setup() {
        coEvery { texasMock.entraIdToken(any(), any()) } returns TexasToken("test-token")
    }

    @Test
    fun `should properly deserialize response`() = testApplication {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/person", request.url.fullPath)
            request.headers["Authorization"] shouldBe "Bearer test-token"

            respond(
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                content = ByteReadChannel(goodResponseBodyJson),
            )
        }

        val pdlClient =
            PdlCloudClient(
                httpClient = HttpClient(mockEngine) {},
                texasClient = texasMock,
                environment = simpleUnitTestEnvironment,
            )

        val response =
            pdlClient.getPerson("hello").getOrElse { fail("Failed to get person from PDL") }

        response.foedselsdato shouldBe LocalDate.now().minusYears(35)
    }

    @Test
    fun `404 should result in not found`() = testApplication {
        val mockEngine = MockEngine { request ->
            assertEquals("/api/person", request.url.fullPath)
            request.headers["Authorization"] shouldBe "Bearer test-token"

            respondError(HttpStatusCode.NotFound)
        }

        val pdlClient =
            PdlCloudClient(
                httpClient = HttpClient(mockEngine) {},
                texasClient = texasMock,
                environment = simpleUnitTestEnvironment,
            )

        val response = pdlClient.getPerson("hello").leftOrNull()
        response shouldBe PdlClient.PdlErrors.NotFound
    }
}
