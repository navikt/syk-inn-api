package no.nav.tsm

import io.kotest.assertions.assertSoftly
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.ktor.client.HttpClient
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.HttpResponse
import io.ktor.http.*
import io.ktor.server.testing.*
import java.time.Duration
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.modules.behandler.payloads.BehandlerOpprettSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerOpprettSykmelding.BehandlerMeta
import no.nav.tsm.modules.behandler.payloads.BehandlerOpprettSykmelding.Values
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingAktivitet
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskHenvisningRecord
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.utils.KafkaTestConsumer
import no.nav.tsm.utils.KafkaTestConsumer.createTestConsumer
import no.nav.tsm.utils.KafkaTestUtils
import no.nav.tsm.utils.WithAll
import no.nav.tsm.utils.configureFullIntegrationTests
import no.nav.tsm.utils.testClient
import no.nav.tsm.utils.testJsonObjectMapper
import no.nav.tsm.utils.uuid

class EverythingTest : WithAll() {
    /** All tests use a single consumer to reduce setup and teardown time */
    companion object {
        private val allRecords: MutableMap<UUID, SykmeldingRecord?> = mutableMapOf()
        private val allPIKs: MutableMap<UUID, JuridiskHenvisningRecord?> = mutableMapOf()
        private val consumer = createTestConsumer(kafka)
    }

    private fun ApplicationTestBuilder.configureEverythingTest() {
        client = testClient()

        application { configureFullIntegrationTests(postgres, kafka) }
    }

    private suspend fun HttpClient.postSykmelding(payload: String): HttpResponse =
        this.post("/api/sykmelding") {
            headers { append("Content-Type", "application/json") }
            setBody(payload)
        }

    private suspend fun HttpClient.getAllSykmeldingerFor(ident: String, hpr: String) =
        this.get("/api/sykmelding") {
                headers {
                    append("Content-Type", "application/json")
                    append("Ident", ident)
                    append("HPR", hpr)
                }
            }
            .let {
                it.status shouldEqual HttpStatusCode.OK

                requireNotNull(it.body<List<BehandlerSykmelding>>())
            }

    @Test
    fun `API should accept a full payload, and properly map and store all values in the database`() =
        testApplication {
            configureEverythingTest()

            val response = client.postSykmelding(Testdata.fullExampleSykmeldingPayload)
            response.status shouldBe HttpStatusCode.OK

            val created = requireNotNull(response.body<BehandlerSykmeldingFull>())
            created.meta.pasient.ident shouldBe "21037712323"
            created.meta.sykmelder.hpr shouldBe "9144889"
            created.meta.legekontorOrgnr shouldBe "123456789"

            val allSykmeldinger =
                client.getAllSykmeldingerFor(ident = "21037712323", hpr = "9144889")
            allSykmeldinger.shouldHaveSize(1)

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
                values.annenFravarsgrunn shouldBe AnnenFravarsgrunn.ARBEIDSRETTET_TILTAK

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
                values.utdypendeSporsmal?.medisinskOppsummering?.sporsmalstekst shouldBe null
                values.utdypendeSporsmal?.medisinskOppsummering?.svar shouldBe null
                values.utdypendeSporsmal?.hensynPaArbeidsplassen?.sporsmalstekst shouldBe
                    "Hensyn på arbeidsplassen"
                values.utdypendeSporsmal?.hensynPaArbeidsplassen?.svar shouldBe
                    "Behov for tilrettelagt arbeidsplass og redusert arbeidsbelastning"
                values.utdypendeSporsmal?.sykdomsutvikling?.sporsmalstekst shouldBe null
                values.utdypendeSporsmal?.sykdomsutvikling?.svar shouldBe null
                values.utdypendeSporsmal?.arbeidsrelaterteUtfordringer?.sporsmalstekst shouldBe null
                values.utdypendeSporsmal?.arbeidsrelaterteUtfordringer?.svar shouldBe null
                values.utdypendeSporsmal?.behandlingOgFremtidigArbeid?.sporsmalstekst shouldBe
                    "Behandling og fremtidig arbeid"
                values.utdypendeSporsmal?.behandlingOgFremtidigArbeid?.svar shouldBe
                    "Pågående behandling forventes å bedre arbeidsevnen på sikt"
                values.utdypendeSporsmal?.uavklarteForhold?.sporsmalstekst shouldBe
                    "Uavklarte forhold"
                values.utdypendeSporsmal?.uavklarteForhold?.svar shouldBe
                    "Videre utredning pågår for å avklare diagnosen"
                values.utdypendeSporsmal?.oppdatertMedisinskStatus?.sporsmalstekst shouldBe null
                values.utdypendeSporsmal?.oppdatertMedisinskStatus?.svar shouldBe null
                values.utdypendeSporsmal?.realistiskMestringArbeid?.sporsmalstekst shouldBe
                    "Realistisk mestring av arbeid"
                values.utdypendeSporsmal?.realistiskMestringArbeid?.svar shouldBe
                    "Delvis arbeidsevne forventes om 3-6 måneder"
                values.utdypendeSporsmal?.forventetHelsetilstandUtvikling?.sporsmalstekst shouldBe
                    "Forventet utvikling av helsetilstand"
                values.utdypendeSporsmal?.forventetHelsetilstandUtvikling?.svar shouldBe
                    "Gradvis bedring forventes med riktig behandling og tilrettelegging"
                values.utdypendeSporsmal?.medisinskeHensyn?.sporsmalstekst shouldBe
                    "Medisinske hensyn"
                values.utdypendeSporsmal?.medisinskeHensyn?.svar shouldBe
                    "Unngå tung løfting og langvarig stående arbeid"
            }

            val record: SykmeldingRecord? = consumeUntil(sykmeldingFull.sykmeldingId)
            record.shouldNotBeNull()
            KafkaTestUtils.expectAllValues(sykmeldingFull, record)
        }

    @Test
    fun `A complete sykmelding should be saved and produced to input and juridisk henvising topic`() =
        testApplication {
            configureEverythingTest()

            val response = client.postSykmelding(Testdata.fullExampleSykmeldingPayload)
            response.status shouldEqual HttpStatusCode.OK

            val created = requireNotNull(response.body<BehandlerSykmeldingFull>())
            created.meta.pasient.ident shouldEqual "21037712323"
            created.meta.sykmelder.hpr shouldEqual "9144889"
            created.meta.legekontorOrgnr shouldEqual "123456789"
            created.values.hoveddiagnose?.code shouldEqual "L73"
            created.values.hoveddiagnose?.system?.name shouldEqual "ICPC2"

            val allSykmeldinger =
                client.getAllSykmeldingerFor(ident = "21037712323", hpr = "9144889")
            allSykmeldinger.shouldHaveSize(1)

            val first = allSykmeldinger.first()
            first.shouldBeInstanceOf<BehandlerSykmeldingFull>()

            val record: SykmeldingRecord? = consumeUntil(first.sykmeldingId, waitForJuridisk = true)
            record.shouldNotBeNull()
            KafkaTestUtils.expectAllValues(first, record)

            // Full happy path hits all 4 trees with juridisk henvisninger
            allPIKs[first.sykmeldingId]?.juridiskeVurderinger?.size shouldEqual 4
        }

    @Test
    fun `suspendert behandler should work and be submitted, even though its INVALID`() =
        testApplication {
            configureEverythingTest()

            val response =
                client.postSykmelding(
                    createCreatePayload(
                        meta = Testdata.suspendertBehandlerMeta,
                        values = Testdata.everyValueAnswered,
                    )
                )
            response.status shouldEqual HttpStatusCode.OK

            val created = requireNotNull(response.body<BehandlerSykmeldingFull>())
            created.meta.pasient.ident shouldEqual "21037712323"
            created.meta.sykmelder.hpr shouldEqual "hprButFnrIsSuspended"
            created.utfall.result shouldEqual RuleType.INVALID

            val record: SykmeldingRecord? = consumeUntil(created.sykmeldingId)
            record.shouldNotBeNull()
            KafkaTestUtils.expectAllValues(created, record)
        }

    @Test
    fun `super minimal sykmelding`() = testApplication {
        configureEverythingTest()

        val response =
            client.postSykmelding(
                createCreatePayload(
                    meta = Testdata.simpleBehandlerMeta,
                    values =
                        Values(
                            pasientenSkalSkjermes = false,
                            svangerskapsrelatert = false,
                            hoveddiagnose =
                                BehandlerOpprettSykmelding.DiagnoseInfo(
                                    system = SykInnDiagnoseSystem.ICPC2,
                                    code = "L73",
                                ),
                            bidiagnoser = emptyList(),
                            aktivitet =
                                listOf(
                                    BehandlerOpprettSykmelding.Aktivitet.Gradert(
                                        fom = LocalDate.now(),
                                        tom = LocalDate.now().plusDays(7),
                                        grad = 70,
                                        reisetilskudd = false,
                                    )
                                ),
                            meldinger =
                                BehandlerOpprettSykmelding.Meldinger(
                                    tilNav = null,
                                    tilArbeidsgiver = null,
                                ),
                            utdypendeSporsmal = null,
                            yrkesskade = null,
                            arbeidsgiver = null,
                            tilbakedatering = null,
                            annenFravarsgrunn = null,
                        ),
                )
            )
        response.status shouldEqual HttpStatusCode.OK

        val created = requireNotNull(response.body<BehandlerSykmeldingFull>())
        created.utfall.result shouldEqual RuleType.OK

        val record = consumeUntil(created.sykmeldingId, waitForJuridisk = true)
        KafkaTestUtils.expectAllValues(created, record)

        val juridisk = requireNotNull(allPIKs[created.sykmeldingId]?.juridiskeVurderinger)
        juridisk.shouldHaveSize(4)
        juridisk.first().kilde shouldBe "syk-inn-api"
        juridisk.first().version shouldBe "1.0.0"
        juridisk.first().versjonAvKode shouldBe "testy-v0"
    }

    /**
     * Consumes until the record has been produced on the Kafka topic, times out after 10 seconds.
     */
    private suspend fun consumeUntil(sykmeldingId: UUID, waitForJuridisk: Boolean = false) =
        withContext(Dispatchers.IO) {
            if (allRecords.containsKey(sykmeldingId)) return@withContext allRecords[sykmeldingId]

            val stopAt = OffsetDateTime.now().plusSeconds(10)
            while (OffsetDateTime.now().isBefore(stopAt)) {
                val records = consumer.poll(Duration.ofMillis(500))
                if (records.isEmpty) continue

                records.forEach { record ->
                    val keyUuid = UUID.fromString(record.key())
                    val value = record.value()

                    when (record.topic()) {
                        KafkaTestConsumer.PIK_TOPIC -> {
                            allPIKs[keyUuid] = KafkaTestConsumer.parsePIKRecord(value)
                        }

                        KafkaTestConsumer.INPUT_TOPIC -> {
                            allRecords[keyUuid] = KafkaTestConsumer.parseSykmeldingRecord(value)
                        }

                        else -> throw IllegalStateException("Unknown topic: ${record.topic()}")
                    }
                }

                if (waitForJuridisk) {
                    if (allRecords.containsKey(sykmeldingId) && allPIKs.containsKey(sykmeldingId)) {
                        return@withContext allRecords[sykmeldingId]
                    }
                } else if (allRecords.containsKey(sykmeldingId)) {
                    return@withContext allRecords[sykmeldingId]
                }
            }

            throw IllegalStateException(
                "Did not receive expected message with id $sykmeldingId within 10 seconds, there were ${allRecords.size} records."
            )
        }
}

private fun createCreatePayload(
    submitId: UUID = UUID.randomUUID(),
    meta: BehandlerMeta,
    values: Values,
): String {
    val paylaod =
        BehandlerOpprettSykmelding.Payload(submitId = submitId, meta = meta, values = values)

    return testJsonObjectMapper.writeValueAsString(paylaod)
}

object Testdata {
    val suspendertBehandlerMeta =
        BehandlerMeta(
            source = "Source (FHIR)",
            pasientIdent = "21037712323",
            sykmelderHpr = "hprButFnrIsSuspended",
            legekontorOrgnr = "123456789",
            legekontorTlf = "12345678",
        )

    val simpleBehandlerMeta =
        BehandlerMeta(
            source = "Source (FHIR)",
            pasientIdent = "21037712323",
            sykmelderHpr = "9144889",
            legekontorOrgnr = "123456789",
            legekontorTlf = "12345678",
        )

    val everyValueAnswered =
        Values(
            pasientenSkalSkjermes = true,
            hoveddiagnose =
                BehandlerOpprettSykmelding.DiagnoseInfo(
                    system = SykInnDiagnoseSystem.ICPC2,
                    code = "L73",
                ),
            bidiagnoser =
                listOf(
                    BehandlerOpprettSykmelding.DiagnoseInfo(
                        system = SykInnDiagnoseSystem.ICPC2,
                        code = "P74",
                    )
                ),
            aktivitet =
                listOf(
                    BehandlerOpprettSykmelding.Aktivitet.IkkeMulig(
                        fom = LocalDate.now(),
                        tom = LocalDate.now(),
                        arbeidsrelatertArsak =
                            BehandlerOpprettSykmelding.ArbeidsrelatertArsak(
                                isArbeidsrelatertArsak = true,
                                arbeidsrelaterteArsaker =
                                    listOf(ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING),
                                annenArbeidsrelatertArsak =
                                    "Begrunnelse for annen arbeidsrelatert årsak",
                            ),
                    )
                ),
            svangerskapsrelatert = true,
            meldinger =
                BehandlerOpprettSykmelding.Meldinger(
                    tilNav = "Dette er en melding til Nav",
                    tilArbeidsgiver = "Dette er en melding til arbeidsgiver",
                ),
            yrkesskade =
                BehandlerOpprettSykmelding.Yrkesskade(
                    yrkesskade = true,
                    skadedato = LocalDate.of(2024, 1, 1),
                ),
            arbeidsgiver =
                BehandlerOpprettSykmelding.Arbeidsgiver(
                    harFlere = true,
                    arbeidsgivernavn = "Arbeidsgiver AS",
                ),
            tilbakedatering =
                BehandlerOpprettSykmelding.Tilbakedatering(
                    startdato = LocalDate.parse("2024-01-01"),
                    begrunnelse = "Begrunnelse for tilbakedatering",
                ),
            annenFravarsgrunn = AnnenFravarsgrunn.ARBEIDSRETTET_TILTAK,
            utdypendeSporsmal =
                BehandlerOpprettSykmelding.UtdypendeSporsmal(
                    utfordringerMedArbeid =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Beskriv utfordringer med arbeid",
                            svar =
                                "Pasienten har betydelige utfordringer med å utføre arbeidsoppgaver",
                        ),
                    hensynPaArbeidsplassen =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Hensyn på arbeidsplassen",
                            svar =
                                "Behov for tilrettelagt arbeidsplass og redusert arbeidsbelastning",
                        ),
                    medisinskOppsummering =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Medisinsk oppsummering",
                            svar = "Pasienten har en kronisk tilstand som krever behandling",
                        ),
                    sykdomsutvikling =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Beskriv sykdomsutviklingen",
                            svar = "Tilstanden har gradvis forverret seg over de siste månedene",
                        ),
                    arbeidsrelaterteUtfordringer =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Arbeidsrelaterte utfordringer",
                            svar = "Fysisk krevende arbeidsoppgaver forverrer tilstanden",
                        ),
                    behandlingOgFremtidigArbeid =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Behandling og fremtidig arbeid",
                            svar = "Pågående behandling forventes å bedre arbeidsevnen på sikt",
                        ),
                    uavklarteForhold =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Uavklarte forhold",
                            svar = "Videre utredning pågår for å avklare diagnosen",
                        ),
                    oppdatertMedisinskStatus =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Oppdatert medisinsk status",
                            svar = "Pasienten responderer moderat på behandlingen",
                        ),
                    realistiskMestringArbeid =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Realistisk mestring av arbeid",
                            svar = "Delvis arbeidsevne forventes om 3-6 måneder",
                        ),
                    forventetHelsetilstandUtvikling =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Forventet utvikling av helsetilstand",
                            svar =
                                "Gradvis bedring forventes med riktig behandling og tilrettelegging",
                        ),
                    medisinskeHensyn =
                        BehandlerOpprettSykmelding.UtdypendeSporsmalSvar(
                            sporsmalstekst = "Medisinske hensyn",
                            svar = "Unngå tung løfting og langvarig stående arbeid",
                        ),
                ),
        )

    val fullExampleSykmeldingPayload =
        createCreatePayload(
            submitId = "495d7f08-f17d-444f-b480-b1c94108d38a".uuid(),
            meta = simpleBehandlerMeta,
            values = everyValueAnswered,
        )
}
