package no.nav.tsm.modules.behandler

import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.types.shouldBeTypeOf
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.server.testing.*
import java.time.LocalDate
import kotlin.test.Test
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingRedacted
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingVerify
import no.nav.tsm.modules.sykmeldinger.configureSykmeldingerModule
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.utils.WithPostgresql
import no.nav.tsm.utils.configurePostgresIntegrationTests
import no.nav.tsm.utils.testClient
import org.intellij.lang.annotations.Language

class BehandlerRoutesTest : WithPostgresql() {
    private fun ApplicationTestBuilder.configureSykmeldingApiTest() {
        client = testClient()

        application {
            configurePostgresIntegrationTests(postgres)

            // Modules in test
            configureSykmeldingerModule()
            configureBehandlerModule()
        }

        runMigrations(true)
        connect()
    }

    @Test
    fun `sanity test - simple create test`() = testApplication {
        configureSykmeldingApiTest()

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(fullExampleSykmeldingPayload)
            }

        val created = requireNotNull(response.body<BehandlerSykmelding>())

        response.status shouldEqual HttpStatusCode.OK
        created.meta.pasient.ident shouldEqual "21037712323"
        created.meta.sykmelder.hpr shouldEqual "9144889"
        created.meta.legekontorOrgnr shouldEqual "123456789"

        // Diagnose
        created.shouldBeTypeOf<BehandlerSykmeldingFull>()
        created.values.hoveddiagnose?.code shouldEqual "L73"
        created.values.hoveddiagnose?.system?.name shouldEqual "ICPC2"

        val allResponse =
            client.get("/api/sykmelding") {
                headers {
                    append("Content-Type", "application/json")
                    append("Ident", "21037712323")
                    append("HPR", "9144889")
                }
            }

        allResponse.status shouldEqual HttpStatusCode.OK

        val allSykmeldinger = requireNotNull(allResponse.body<List<BehandlerSykmelding>>())
        allSykmeldinger shouldHaveSize 1
        allSykmeldinger.first().shouldBeTypeOf<BehandlerSykmeldingFull>()
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
            val initialId = created.sykmeldingId

            response.status shouldEqual HttpStatusCode.OK
            created.meta.pasient.ident shouldEqual "21037712323"

            val nextRequest =
                client.post("/api/sykmelding") {
                    headers { append("Content-Type", "application/json") }
                    setBody(fullExampleSykmeldingPayload)
                }

            val nextResult = requireNotNull(nextRequest.body<BehandlerSykmeldingFull>())
            nextRequest.status shouldEqual HttpStatusCode.OK

            // The same ID means the system didn't generate another ID, but instead returned the one
            // in the DB
            nextResult.sykmeldingId shouldEqual initialId
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

            val created = requireNotNull(response.body<BehandlerSykmelding>())
            created.shouldBeTypeOf<BehandlerSykmeldingFull>()
            response.status shouldEqual HttpStatusCode.OK
            created.meta.sykmelder.hpr shouldEqual "someone-else"

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

            specificSykmelding.shouldBeTypeOf<BehandlerSykmeldingRedacted>()
        }

    @Test
    fun `verify - rule hits should return why`() = testApplication {
        configureSykmeldingApiTest()

        val response =
            client.post("/api/sykmelding/verify") {
                headers { append("Content-Type", "application/json") }
                setBody(fullExampleSykmeldingPayload.replace("L73", "Bad"))
            }

        val created = requireNotNull(response.body<BehandlerSykmeldingVerify>())

        response.status shouldEqual HttpStatusCode.OK
        created.shouldBeTypeOf<BehandlerSykmeldingVerify>()
        created.status shouldEqual RuleType.INVALID
        created.rule shouldEqual "UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE"
    }

    @Test
    fun `Verify - should reply with 422 when person does not exist in PDL`() = testApplication {
        configureSykmeldingApiTest()

        val response =
            client.post("/api/sykmelding/verify") {
                headers { append("Content-Type", "application/json") }
                setBody(fullExampleSykmeldingPayload.replace("21037712323", "does-not-exist"))
            }

        response.status shouldEqual HttpStatusCode.UnprocessableEntity
        response.body<Map<String, String>>()["message"] shouldEqual "Person does not exist"
    }

    @Test
    fun `sanity test - are tests independent`() = testApplication {
        configureSykmeldingApiTest()

        val allResponse =
            client.get("/api/sykmelding") {
                headers {
                    append("Content-Type", "application/json")
                    set("Ident", "21037712323")
                    set("HPR", "123456789")
                }
            }

        allResponse.status shouldEqual HttpStatusCode.OK

        val allSykmeldinger = requireNotNull(allResponse.body<List<BehandlerSykmelding>>())
        allSykmeldinger shouldHaveSize 0
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

            response.status shouldEqual HttpStatusCode.BadRequest
        }

    @Test
    fun `Should NOT automatically downgrade ICPC2B to ICPC2`() = testApplication {
        configureSykmeldingApiTest()

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(
                    fullExampleSykmeldingPayload
                        .replace("ICPC2", "ICPC2B")
                        .replace("L73", "Y99.0004")
                )
            }

        response.status shouldEqual HttpStatusCode.OK

        val result = response.body<BehandlerSykmeldingFull>()
        result.values.hoveddiagnose?.system?.name shouldEqual "ICPC2B"
        result.values.hoveddiagnose?.code shouldEqual "Y99.0004"
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

            response.status shouldEqual HttpStatusCode.BadRequest
        }

    @Test
    fun `Broken input data - should fail with 500 due to invalid sykmelderHpr`() = testApplication {
        configureSykmeldingApiTest()

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(brokenExampleSykmeldingPayloadInvalidSykmelderHpr)
            }

        response.status shouldEqual HttpStatusCode.InternalServerError
    }

    @Test
    fun `Broken input data - should fail with 500 due to invalid sykmelderFnr`() = testApplication {
        configureSykmeldingApiTest()

        val response =
            client.post("/api/sykmelding") {
                headers { append("Content-Type", "application/json") }
                setBody(brokenExampleSykmeldingPayloadValidHprButBrokenFnr)
            }

        response.status shouldEqual HttpStatusCode.InternalServerError
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

            response.status shouldEqual HttpStatusCode.OK
            response.body<BehandlerSykmeldingFull>().utfall.result shouldEqual RuleType.INVALID
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
