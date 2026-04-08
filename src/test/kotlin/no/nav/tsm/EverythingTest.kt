package no.nav.tsm

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
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
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingAktivitet
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.behandler.payloads.SykInnDiagnoseSystem
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.RuleType
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
    fun `API should accept and return all values through database`() = testApplication {
        configureSykmeldingApiTest()

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(fullExampleSykmeldingPayload)
            }

        val created = requireNotNull(response.body<BehandlerSykmeldingFull>())

        response.status shouldBe HttpStatusCode.OK
        created.meta.pasient.ident shouldBe "21037712323"
        created.meta.sykmelder.hpr shouldBe "9144889"
        created.meta.legekontorOrgnr shouldBe "123456789"

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

        val sykmelding = requireNotNull(allSykmeldinger.first())
        val sykmeldingFull = sykmelding.shouldBeInstanceOf<BehandlerSykmeldingFull>()

        assertSoftly(sykmeldingFull) {
            // meta
            meta.pasient.ident shouldBe "21037712323"
            meta.sykmelder.hpr shouldBe "9144889"
            meta.legekontorOrgnr shouldBe "123456789"
            meta.legekontorTlf shouldBe "12345678"

            // values
            values.pasientenSkalSkjermes shouldBe true
            values.svangerskapsrelatert shouldBe true

            // hoveddiagnose
            values.hoveddiagnose?.code shouldBe "L73"
            values.hoveddiagnose?.system shouldBe SykInnDiagnoseSystem.ICPC2

            // bidiagnoser
            values.bidiagnoser?.first()?.code shouldBe "P74"
            values.bidiagnoser?.first()?.system shouldBe SykInnDiagnoseSystem.ICPC2

            // aktivitet
            values.aktivitet shouldHaveSize 1
            if (values.aktivitet.size > 1) {
                val aktivitet = values.aktivitet.first()
                aktivitet.shouldBeInstanceOf<BehandlerSykmeldingAktivitet.IkkeMulig>()
                aktivitet.fom shouldBe LocalDate.now()
                aktivitet.tom shouldBe LocalDate.now()
                aktivitet.medisinskArsak.isMedisinskArsak shouldBe true
                aktivitet.arbeidsrelatertArsak.isArbeidsrelatertArsak shouldBe true
                aktivitet.arbeidsrelatertArsak.arbeidsrelaterteArsaker shouldBe
                    listOf(ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING)
                aktivitet.arbeidsrelatertArsak.annenArbeidsrelatertArsak shouldBe
                    "Begrunnelse for annen arbeidsrelatert årsak"
            }

            // utfall
            utfall.result shouldBe RuleType.OK
            utfall.cause shouldBe null

            // meldinger
            values.meldinger?.tilNav shouldBe "Dette er en melding til Nav"
            values.meldinger?.tilArbeidsgiver shouldBe "Dette er en melding til arbeidsgiver"

            // yrkesskade
            values.yrkesskade?.yrkesskade shouldBe true
            values.yrkesskade?.skadedato shouldBe LocalDate.of(2024, 1, 1)

            // arbeidsgiver
            values.arbeidsgiver?.harFlere shouldBe true
            values.arbeidsgiver?.arbeidsgivernavn shouldBe "Arbeidsgiver AS"

            // tilbakedatering
            values.tilbakedatering?.startdato shouldBe LocalDate.of(2024, 1, 1)
            values.tilbakedatering?.begrunnelse shouldBe "Begrunnelse for tilbakedatering"

            // utdypendeSporsmal
            values.utdypendeSporsmal?.utfordringerMedArbeid?.sporsmalstekst shouldBe
                "Beskriv utfordringer med arbeid"
            values.utdypendeSporsmal?.utfordringerMedArbeid?.svar shouldBe
                "Pasienten har betydelige utfordringer med å utføre arbeidsoppgaver"
            values.utdypendeSporsmal?.medisinskOppsummering?.sporsmalstekst shouldBe
                "Medisinsk oppsummering"
            values.utdypendeSporsmal?.medisinskOppsummering?.svar shouldBe
                "Pasienten har en kronisk tilstand som krever behandling"
            values.utdypendeSporsmal?.hensynPaArbeidsplassen?.sporsmalstekst shouldBe
                "Hensyn på arbeidsplassen"
            values.utdypendeSporsmal?.hensynPaArbeidsplassen?.svar shouldBe
                "Behov for tilrettelagt arbeidsplass og redusert arbeidsbelastning"
            values.utdypendeSporsmal?.sykdomsutvikling?.sporsmalstekst shouldBe
                "Beskriv sykdomsutviklingen"
            values.utdypendeSporsmal?.sykdomsutvikling?.svar shouldBe
                "Tilstanden har gradvis forverret seg over de siste månedene"
            values.utdypendeSporsmal?.arbeidsrelaterteUtfordringer?.sporsmalstekst shouldBe
                "Arbeidsrelaterte utfordringer"
            values.utdypendeSporsmal?.arbeidsrelaterteUtfordringer?.svar shouldBe
                "Fysisk krevende arbeidsoppgaver forverrer tilstanden"
            values.utdypendeSporsmal?.behandlingOgFremtidigArbeid?.sporsmalstekst shouldBe
                "Behandling og fremtidig arbeid"
            values.utdypendeSporsmal?.behandlingOgFremtidigArbeid?.svar shouldBe
                "Pågående behandling forventes å bedre arbeidsevnen på sikt"
            values.utdypendeSporsmal?.uavklarteForhold?.sporsmalstekst shouldBe "Uavklarte forhold"
            values.utdypendeSporsmal?.uavklarteForhold?.svar shouldBe
                "Videre utredning pågår for å avklare diagnosen"
            values.utdypendeSporsmal?.oppdatertMedisinskStatus?.sporsmalstekst shouldBe
                "Oppdatert medisinsk status"
            values.utdypendeSporsmal?.oppdatertMedisinskStatus?.svar shouldBe
                "Pasienten responderer moderat på behandlingen"
            values.utdypendeSporsmal?.realistiskMestringArbeid?.sporsmalstekst shouldBe
                "Realistisk mestring av arbeid"
            values.utdypendeSporsmal?.realistiskMestringArbeid?.svar shouldBe
                "Delvis arbeidsevne forventes om 3-6 måneder"
            values.utdypendeSporsmal?.forventetHelsetilstandUtvikling?.sporsmalstekst shouldBe
                "Forventet utvikling av helsetilstand"
            values.utdypendeSporsmal?.forventetHelsetilstandUtvikling?.svar shouldBe
                "Gradvis bedring forventes med riktig behandling og tilrettelegging"
            values.utdypendeSporsmal?.medisinskeHensyn?.sporsmalstekst shouldBe "Medisinske hensyn"
            values.utdypendeSporsmal?.medisinskeHensyn?.svar shouldBe
                "Unngå tung løfting og langvarig stående arbeid"
        }
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
    |    "pasientenSkalSkjermes": true,
    |    "hoveddiagnose": {
    |      "system": "ICPC2",
    |      "code": "L73"
    |    },
    |    "bidiagnoser": [
    |      {
    |        "system": "ICPC2",
    |        "code": "P74"
    |      }
    |    ],
    |    "aktivitet": [
    |      {
    |        "type": "AKTIVITET_IKKE_MULIG",
    |        "fom": "${LocalDate.now()}",
    |        "tom": "${LocalDate.now()}",
    |        "medisinskArsak": {
    |          "isMedisinskArsak": true
    |        },
    |        "arbeidsrelatertArsak": {
    |          "isArbeidsrelatertArsak": true,
    |          "arbeidsrelaterteArsaker": [
    |            "MANGLENDE_TILRETTELEGGING"
    |          ],
    |          "annenArbeidsrelatertArsak": "Begrunnelse for annen arbeidsrelatert årsak"
    |        }
    |      }
    |    ],
    |    "meldinger": {
    |      "tilNav": "Dette er en melding til Nav",
    |      "tilArbeidsgiver": "Dette er en melding til arbeidsgiver"
    |    },
    |    "svangerskapsrelatert": true,
    |    "yrkesskade": {
    |      "yrkesskade": true,
    |      "skadedato": "2024-01-01"
    |    },
    |    "arbeidsgiver": {
    |      "harFlere": true,
    |      "arbeidsgivernavn": "Arbeidsgiver AS"
    |    },
    |    "tilbakedatering": {
    |      "begrunnelse": "Begrunnelse for tilbakedatering",
    |      "startdato": "2024-01-01"
    |    },
    |    "utdypendeSporsmal": {
    |      "utfordringerMedArbeid": {
    |        "sporsmalstekst": "Beskriv utfordringer med arbeid",
    |        "svar": "Pasienten har betydelige utfordringer med å utføre arbeidsoppgaver"
    |      },
    |      "medisinskOppsummering": {
    |        "sporsmalstekst": "Medisinsk oppsummering",
    |        "svar": "Pasienten har en kronisk tilstand som krever behandling"
    |      },
    |      "hensynPaArbeidsplassen": {
    |        "sporsmalstekst": "Hensyn på arbeidsplassen",
    |        "svar": "Behov for tilrettelagt arbeidsplass og redusert arbeidsbelastning"
    |      },
    |      "sykdomsutvikling": {
    |        "sporsmalstekst": "Beskriv sykdomsutviklingen",
    |        "svar": "Tilstanden har gradvis forverret seg over de siste månedene"
    |      },
    |      "arbeidsrelaterteUtfordringer": {
    |        "sporsmalstekst": "Arbeidsrelaterte utfordringer",
    |        "svar": "Fysisk krevende arbeidsoppgaver forverrer tilstanden"
    |      },
    |      "behandlingOgFremtidigArbeid": {
    |        "sporsmalstekst": "Behandling og fremtidig arbeid",
    |        "svar": "Pågående behandling forventes å bedre arbeidsevnen på sikt"
    |      },
    |      "uavklarteForhold": {
    |        "sporsmalstekst": "Uavklarte forhold",
    |        "svar": "Videre utredning pågår for å avklare diagnosen"
    |      },
    |      "oppdatertMedisinskStatus": {
    |        "sporsmalstekst": "Oppdatert medisinsk status",
    |        "svar": "Pasienten responderer moderat på behandlingen"
    |      },
    |      "realistiskMestringArbeid": {
    |        "sporsmalstekst": "Realistisk mestring av arbeid",
    |        "svar": "Delvis arbeidsevne forventes om 3-6 måneder"
    |      },
    |      "forventetHelsetilstandUtvikling": {
    |        "sporsmalstekst": "Forventet utvikling av helsetilstand",
    |        "svar": "Gradvis bedring forventes med riktig behandling og tilrettelegging"
    |      },
    |      "medisinskeHensyn": {
    |        "sporsmalstekst": "Medisinske hensyn",
    |        "svar": "Unngå tung løfting og langvarig stående arbeid"
    |      }
    |    }
    |  }
    |}
"""
        .trimMargin()
