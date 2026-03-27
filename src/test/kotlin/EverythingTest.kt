import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.headers
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.utils.WithAll
import no.nav.tsm.utils.configureFullIntegrationTests
import no.nav.tsm.utils.testClient
import org.intellij.lang.annotations.Language

class EverythingTest : WithAll() {
    private fun ApplicationTestBuilder.configureSykmeldingApiTest() {
        client = testClient()

        application { configureFullIntegrationTests(postgres, kafka) }
    }

    @Test
    fun `simple API to Kafka test`() = testApplication {
        configureSykmeldingApiTest()

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(fullExampleSykmeldingPayload)
            }

        val created = requireNotNull(response.body<BehandlerSykmeldingFull>())

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(created.meta.pasient.ident, "21037712323")
        assertEquals(created.meta.sykmelder.hpr, "9144889")
        assertEquals(created.meta.legekontorOrgnr, "123456789")

        // Diagnose
        assertEquals(created.values.hoveddiagnose?.code, "L73")
        assertEquals(created.values.hoveddiagnose?.system?.name, "ICPC2")

        val allResponse =
            client.get("/api/sykmelding") {
                headers {
                    append("Content-Type", "application/json")
                    append("Ident", "21037712323")
                    append("HPR", "9144889")
                }
            }

        assertEquals(HttpStatusCode.OK, allResponse.status)

        val allSykmeldinger = requireNotNull(allResponse.body<List<BehandlerSykmelding>>())
        assertEquals(1, allSykmeldinger.size)

        // TODO: Somehow assert that the message is published to Kafka
    }
}

@Language("JSON")
private val fullExampleSykmeldingPayload =
    """
    |{
    |  "submitId": "495d7f08-f17d-444f-b480-b1c94108d38a",
    |  "meta": {
    |    "source": "Source (FHIR)",
    |    "pasientIdent": "21037712323",
    |    "sykmelderHpr": "9144889",
    |    "legekontorOrgnr": "123456789",
    |    "legekontorTlf": "12345678"
    |  },
    |  "values": {
    |    "pasientenSkalSkjermes": false,
    |    "hoveddiagnose": {
    |      "system": "ICPC2",
    |      "code": "L73"
    |    },
    |    "bidiagnoser": [],
    |    "aktivitet": [
    |      {
    |        "type": "AKTIVITET_IKKE_MULIG",
    |        "fom": "${LocalDate.now()}",
    |        "tom": "${LocalDate.now()}",
    |        "medisinskArsak": {"isMedisinskArsak":  true},
    |        "arbeidsrelatertArsak": {"isArbeidsrelatertArsak":  false, "arbeidsrelaterteArsaker":  [], "annenArbeidsrelatertArsak":  null}
    |      }
    |    ],
    |    "meldinger": {
    |      "tilNav": null,
    |      "tilArbeidsgiver": null
    |    },
    |    "svangerskapsrelatert": false,
    |    "yrkesskade": null,
    |    "arbeidsgiver": null,
    |    "tilbakedatering": null
    |  }
    |}
"""
        .trimMargin()
