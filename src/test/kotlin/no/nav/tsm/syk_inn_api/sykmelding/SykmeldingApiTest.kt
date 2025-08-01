package no.nav.tsm.syk_inn_api.sykmelding

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.test.FullIntegrationTest
import org.intellij.lang.annotations.Language
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.boot.test.web.client.exchange
import org.springframework.boot.test.web.client.postForEntity
import org.springframework.http.*

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SykmeldingApiTest(@param:Autowired val restTemplate: TestRestTemplate) :
    FullIntegrationTest() {

    @Test
    fun `sanity test - simple create test`() {
        val response =
            restTemplate.postForEntity<SykmeldingDocument>(
                "/api/sykmelding",
                HttpEntity(
                    fullExampleSykmeldingPayload,
                    HttpHeaders().apply {
                        set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    },
                ),
            )

        val created = requireNotNull(response.body)

        assertEquals(HttpStatus.CREATED, response.statusCode)
        assertEquals(created.meta.pasientIdent, "21037712323")
        assertEquals(created.meta.sykmelder.hprNummer, "9144889")
        assertEquals(created.meta.legekontorOrgnr, "123456789")

        // Diagnose
        assertEquals(created.values.hoveddiagnose?.code, "L73")
        assertEquals(created.values.hoveddiagnose?.system?.name, "ICPC2")

        val allResponse =
            restTemplate.exchange<List<SykmeldingDocument>>(
                "/api/sykmelding",
                HttpMethod.GET,
                HttpEntity<Void>(
                    HttpHeaders().apply {
                        set("Ident", "21037712323")
                        set("HPR", "123456789")
                    },
                ),
            )

        assertEquals(HttpStatus.OK, allResponse.statusCode)

        val allSykmeldinger = requireNotNull(allResponse.body)
        assertEquals(1, allSykmeldinger.size)
    }

    @Test
    fun `rule hits should return why`() {
        val response =
            restTemplate.postForEntity<CreateSykmelding.RuleOutcome>(
                "/api/sykmelding",
                HttpEntity(
                    fullExampleSykmeldingPayload.replace("L73", "Bad"),
                    HttpHeaders().apply {
                        set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    },
                ),
            )

        val created = requireNotNull(response.body)

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
        assertEquals(created.status, "INVALID")
        assertEquals(created.rule, "UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE")
    }

    @Test
    fun `sanity test - are tests independent`() {
        val allResponse =
            restTemplate.exchange<List<SykmeldingDocument>>(
                "/api/sykmelding",
                HttpMethod.GET,
                HttpEntity<Void>(
                    HttpHeaders().apply {
                        set("Ident", "21037712323")
                        set("HPR", "123456789")
                    },
                ),
            )

        assertEquals(HttpStatus.OK, allResponse.statusCode)

        val allSykmeldinger = requireNotNull(allResponse.body)
        assertEquals(0, allSykmeldinger.size)
    }

    @Test
    fun `Broken input data - should fail with 400 due to invalid DiagnoseSystem`() {
        val response =
            restTemplate.postForEntity<String>(
                "/api/sykmelding",
                HttpEntity(
                    brokenExampleSykmeldingPayloadBadDiagnoseSystem,
                    HttpHeaders().apply {
                        set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    },
                ),
            )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Broken input data - should fail with 400 due to unknown Aktivitet type`() {
        val response =
            restTemplate.postForEntity<String>(
                "/api/sykmelding",
                HttpEntity(
                    brokenExampleSykmeldingPayloadUnknownAktivitet,
                    HttpHeaders().apply {
                        set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    },
                ),
            )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
    }

    @Test
    fun `Broken input data - should fail with 500 due to invalid sykmelderHpr`() {
        val response =
            restTemplate.postForEntity<String>(
                "/api/sykmelding",
                HttpEntity(
                    brokenExampleSykmeldingPayloadInvalidSykmelderHpr,
                    HttpHeaders().apply {
                        set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    },
                ),
            )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `Broken input data - should fail with 500 due to invalid sykmelderFnr`() {
        val response =
            restTemplate.postForEntity<String>(
                "/api/sykmelding",
                HttpEntity(
                    brokenExampleSykmeldingPayloadValidHprButBrokenFnr,
                    HttpHeaders().apply {
                        set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    },
                ),
            )

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `Broken input data - rule hit - should fail with 422 due to sykmelder being suspended`() {
        val response =
            restTemplate.postForEntity<String>(
                "/api/sykmelding",
                HttpEntity(
                    brokenExampleSykmeldingPayloadValidHprButSuspendedFnr,
                    HttpHeaders().apply {
                        set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                    },
                ),
            )

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.statusCode)
    }
}

@Language("JSON")
private val fullExampleSykmeldingPayload =
    """
    |{
    |  "meta": {
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
    |  "meta": {
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
    |  "meta": {
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
    |  "meta": {
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
    |  "meta": {
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
private val brokenExampleSykmeldingPayloadValidHprButSuspendedFnr =
    """
    |{
    |  "meta": {
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
