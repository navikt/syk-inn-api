package modules.behandler

import io.ktor.client.call.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.testing.*
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import modules.behandler.payloads.BehandlerSykmelding
import modules.behandler.payloads.BehandlerSykmeldingFull
import modules.behandler.payloads.BehandlerSykmeldingRedacted
import modules.behandler.payloads.BehandlerSykmeldingVerify
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.sykmelding.input.core.model.RuleType
import org.intellij.lang.annotations.Language
import utils.WithPostgresql
import utils.configureIntegrationTestDependencies
import utils.testClient

class SykmeldingApiTest : WithPostgresql() {

    private fun ApplicationTestBuilder.configureSykmeldingApiTest() {
        client = testClient()
        application { configureIntegrationTestDependencies(postgres) }
    }

    @Test
    fun `sanity test - simple create test`() = testApplication {
        configureSykmeldingApiTest()

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(fullExampleSykmeldingPayload)
            }

        val created = requireNotNull(response.body<BehandlerSykmeldingFull>())

        assertEquals(HttpStatusCode.Created, response.status)
        assertEquals(created.meta.pasientIdent, "21037712323")
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
    }

    @Test
    fun `idempotency test - should not create multiple with same idempotency key`() =
        testApplication {
            configureSykmeldingApiTest()

            val response =
                client.post("/api/sykmelding") {
                    headers { append("Content-Type", "application/json") }
                    setBody(fullExampleSykmeldingPayload)
                }

            val created = requireNotNull(response.body<BehandlerSykmeldingFull>())

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(created.meta.pasientIdent, "21037712323")

            val nextRequest =
                client.post("/api/sykmelding") {
                    headers { append("Content-Type", "application/json") }
                    setBody(fullExampleSykmeldingPayload)
                }

            // TODO: Ser ut som 208 er brukt litt feil
            assertEquals(HttpStatusCode(208, "Already Reported"), nextRequest.status)
        }

    @Test
    fun `fetching single sykmelding not written by you should return 'Redacted' version`() =
        testApplication {
            configureSykmeldingApiTest()

            val response =
                client.post("/api/sykmelding") {
                    headers { append("Content-Type", "application/json") }
                    setBody(fullExampleSykmeldingPayload.replace("9144889", "someone-else"))
                }

            val created = requireNotNull(response.body<BehandlerSykmeldingFull>())
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(created.meta.sykmelder.hpr, "someone-else")

            val specificSykmeldingResponse =
                client.get("/api/sykmelding/${created.sykmeldingId}") {
                    headers {
                        append("Content-Type", "application/json")
                        append("Ident", "21037712323")
                        append("HPR", "9144889")
                    }
                }

            val specificSykmelding =
                requireNotNull(specificSykmeldingResponse.body<BehandlerSykmelding>())
            assertIs<BehandlerSykmeldingRedacted>(specificSykmelding)
        }

    @Test
    fun `verify - rule hits should return why`() = testApplication {
        val client = createClient { install(ContentNegotiation) { jackson() } }
        application { configureIntegrationTestDependencies(postgres) }

        val response =
            client.post("/api/sykmelding/verify") {
                headers { append("Content-Type", "application/json") }
                setBody(fullExampleSykmeldingPayload.replace("L73", "Bad"))
            }

        val created = requireNotNull(response.body<BehandlerSykmeldingVerify>())

        assertEquals(HttpStatusCode.OK, response.status)
        assertIs<BehandlerSykmeldingVerify>(created)
        assertEquals(created.status, RegulaOutcomeStatus.INVALID)
        // TODO: /verify må ha rule tree navn og sånt
        // assertEquals(created.rule, "UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE")
    }

    @Test
    fun `Verify - should reply with 422 when person does not exist in PDL`() = testApplication {
        val client = createClient { install(ContentNegotiation) { jackson() } }
        application { configureIntegrationTestDependencies(postgres) }

        val response =
            client.post("/api/sykmelding/verify") {
                headers { append("Content-Type", "application/json") }
                setBody(fullExampleSykmeldingPayload.replace("21037712323", "does-not-exist"))
            }

        assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
        assertEquals(response.body<Map<String, String>>()["message"], "Person does not exist")
    }

    @Test
    fun `sanity test - are tests independent`() = testApplication {
        val client = createClient { install(ContentNegotiation) { jackson() } }
        application { configureIntegrationTestDependencies(postgres) }

        val allResponse =
            client.get("/api/sykmelding") {
                headers {
                    append("Content-Type", "application/json")
                    set("Ident", "21037712323")
                    set("HPR", "123456789")
                }
            }

        assertEquals(HttpStatusCode.OK, allResponse.status)

        val allSykmeldinger = requireNotNull(allResponse.body<List<BehandlerSykmelding>>())
        assertEquals(0, allSykmeldinger.size)
    }

    @Test
    fun `Broken input data - should fail with 400 due to invalid DiagnoseSystem`() =
        testApplication {
            configureSykmeldingApiTest()

            val response =
                client.post("/api/sykmelding") {
                    headers { append("Content-Type", "application/json") }
                    setBody(brokenExampleSykmeldingPayloadBadDiagnoseSystem)
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `Should NOT automatically downgrade ICPC2B to ICPC2`() = testApplication {
        val client = createClient { install(ContentNegotiation) { jackson() } }
        application { configureIntegrationTestDependencies(postgres) }

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(
                    fullExampleSykmeldingPayload
                        .replace("ICPC2", "ICPC2B")
                        .replace("L73", "Y99.0004")
                )
            }

        assertEquals(HttpStatusCode.Created, response.status)

        val result = response.body<BehandlerSykmeldingFull>()
        assertEquals(result.values.hoveddiagnose?.system?.name, "ICPC2B")
        assertEquals(result.values.hoveddiagnose?.code, "Y99.0004")
    }

    @Test
    fun `Broken input data - should fail with 400 due to unknown Aktivitet type`() =
        testApplication {
            configureSykmeldingApiTest()

            val response =
                client.post("/api/sykmelding") {
                    headers { append("Content-Type", "application/json") }
                    setBody(brokenExampleSykmeldingPayloadUnknownAktivitet)
                }

            assertEquals(HttpStatusCode.BadRequest, response.status)
        }

    @Test
    fun `Broken input data - should fail with 500 due to invalid sykmelderHpr`() = testApplication {
        val client = createClient { install(ContentNegotiation) { jackson() } }
        application { configureIntegrationTestDependencies(postgres) }

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(brokenExampleSykmeldingPayloadInvalidSykmelderHpr)
            }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `Broken input data - should fail with 500 due to invalid sykmelderFnr`() = testApplication {
        val client = createClient { install(ContentNegotiation) { jackson() } }
        application { configureIntegrationTestDependencies(postgres) }

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(brokenExampleSykmeldingPayloadValidHprButBrokenFnr)
            }

        assertEquals(HttpStatusCode.InternalServerError, response.status)
    }

    @Test
    fun `Expected rule hit - should OK with 200 even if sykmelder is suspended`() =
        testApplication {
            configureSykmeldingApiTest()

            val response =
                client.post("/api/sykmelding") {
                    headers { append("Content-Type", "application/json") }
                    setBody(exampleSykmeldingPayloadValidHprButSuspendedIdent)
                }

            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(response.body<BehandlerSykmeldingFull>().utfall.result, RuleType.INVALID)
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

@Language("JSON")
private val brokenExampleSykmeldingPayloadBadDiagnoseSystem =
    """
    |{
    |  "submitId": "d06dc9ef-b090-4529-8649-1485d165197c",
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
    |      "system": "ICPC69",
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

@Language("JSON")
private val brokenExampleSykmeldingPayloadUnknownAktivitet =
    """
    |{
    |  "submitId": "fa798f2d-c218-4c38-a40f-9bdd32893f2b",
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
    |        "type": "DENNE TYPEN AKTIVITET FINNES IKKE",
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

@Language("JSON")
private val brokenExampleSykmeldingPayloadInvalidSykmelderHpr =
    """
    |{
    |  "submitId": "f5465ec4-10ea-4123-aa3f-cd47da9d9bfe",
    |  "meta": {
    |  "source": "Source (FHIR)",
    |    "pasientIdent": "21037712323",
    |    "sykmelderHpr": "brokenHpr",
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

@Language("JSON")
private val brokenExampleSykmeldingPayloadValidHprButBrokenFnr =
    """
    |{
    |  "submitId": "6d37ce07-9960-427b-b608-bd1c1d3db7b4",
    |  "meta": {
    |  "source":  "Source (FHIR)",
    |    "pasientIdent": "21037712323",
    |    "sykmelderHpr": "hprButHasBrokenFnrAndNoGodkjenninger",
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

@Language("JSON")
private val exampleSykmeldingPayloadValidHprButSuspendedIdent =
    """
    |{
    |  "submitId": "927a297a-ff4c-481d-9d57-97f681ab79b3",
    |  "meta": {
    |      "source":  "Source (FHIR)",
    |    "pasientIdent": "21037712323",
    |    "sykmelderHpr": "hprButFnrIsSuspended",
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
