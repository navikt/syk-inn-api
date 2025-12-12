package no.nav.tsm.syk_inn_api.sykmelding

import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertNotNull
import kotlin.test.fail
import no.nav.tsm.regulus.regula.RegulaOutcome
import no.nav.tsm.regulus.regula.RegulaOutcomeReason
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.person.Navn
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprKode
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingProducer
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingArbeidsrelatertArsak
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMedisinskArsak
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingPasient
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentArbeidsrelatertArsak
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMedisinskArsak
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeta
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentValues
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import no.nav.tsm.sykmelding.input.core.model.RuleType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SykmeldingServiceTest {
    private lateinit var sykmeldingService: SykmeldingService
    private lateinit var sykmelderService: SykmelderService
    private lateinit var ruleService: RuleService
    private lateinit var sykmeldingInputProducer: SykmeldingProducer
    private lateinit var sykmeldingPersistenceService: SykmeldingPersistenceService
    private lateinit var personService: PersonService

    val pasientIdent = "01019078901"
    val behandlerIdent = "0101197054321"
    val behandlerHpr = "123456789"
    val sykmeldingId = UUID.randomUUID().toString()
    val foedselsdato = LocalDate.of(1990, 1, 1)
    val navn = Navn(fornavn = "Ola", mellomnavn = null, etternavn = "Nordmann")

    @BeforeEach
    fun setup() {
        ruleService = mockk()
        sykmeldingPersistenceService = mockk(relaxed = true)
        sykmeldingInputProducer = mockk()
        personService = mockk()
        sykmelderService = mockk()
        sykmeldingService =
            SykmeldingService(
                sykmeldingPersistenceService = sykmeldingPersistenceService,
                ruleService = ruleService,
                sykmelderService = sykmelderService,
                sykmeldingInputProducer = sykmeldingInputProducer,
                personService = personService,
            )
    }

    @Test
    fun `create sykmelding with valid data`() {
        every { sykmelderService.sykmelderMedSuspensjon(behandlerHpr, any(), any()) } returns
            Result.success(
                Sykmelder.MedSuspensjon(
                    hpr = behandlerHpr,
                    ident = pasientIdent,
                    navn =
                        Navn(
                            fornavn = "Ola",
                            mellomnavn = null,
                            etternavn = "Nordmann",
                        ),
                    godkjenninger =
                        listOf(
                            HprGodkjenning(
                                helsepersonellkategori =
                                    HprKode(
                                        aktiv = true,
                                        oid = 0,
                                        verdi = "LE",
                                    ),
                                autorisasjon =
                                    HprKode(
                                        aktiv = true,
                                        oid = 7704,
                                        verdi = "1",
                                    ),
                                tillegskompetanse = null,
                            ),
                        ),
                    suspendert = false,
                ),
            )

        every { personService.getPersonByIdent(any()) } returns
            Result.success(Person(navn = navn, fodselsdato = foedselsdato, ident = "123"))
        every { ruleService.validateRules(any(), any(), any(), foedselsdato) } returns
            RegulaResult.Ok(
                emptyList(),
            )

        val sykmeldingDocument =
            SykmeldingDocument(
                sykmeldingId = sykmeldingId,
                meta =
                    SykmeldingDocumentMeta(
                        mottatt = OffsetDateTime.now(),
                        pasientIdent = pasientIdent,
                        sykmelder =
                            SykmeldingDocumentSykmelder(
                                hprNummer = behandlerHpr,
                                fornavn = "Magnar",
                                mellomnavn = null,
                                etternavn = "Koman"
                            ),
                        legekontorOrgnr = "987654321",
                        legekontorTlf = "12345678",
                    ),
                values =
                    SykmeldingDocumentValues(
                        hoveddiagnose =
                            SykmeldingDocumentDiagnoseInfo(
                                system = DiagnoseSystem.ICD10,
                                code = "Z01",
                                text = "Ukjent diagnose",
                            ),
                        aktivitet =
                            listOf(
                                SykmeldingDocumentAktivitet.IkkeMulig(
                                    fom = LocalDate.parse("2020-01-01"),
                                    tom = LocalDate.parse("2020-01-30"),
                                    medisinskArsak =
                                        SykmeldingDocumentMedisinskArsak(isMedisinskArsak = true),
                                    arbeidsrelatertArsak =
                                        SykmeldingDocumentArbeidsrelatertArsak(
                                            isArbeidsrelatertArsak = false,
                                            arbeidsrelaterteArsaker = emptyList(),
                                            annenArbeidsrelatertArsak = null
                                        )
                                ),
                            ),
                        bidiagnoser = emptyList(),
                        svangerskapsrelatert = false,
                        pasientenSkalSkjermes = false,
                        meldinger =
                            SykmeldingDocumentMeldinger(
                                tilNav = null,
                                tilArbeidsgiver = null,
                            ),
                        yrkesskade = null,
                        arbeidsgiver = null,
                        tilbakedatering = null,
                        utdypendeSporsmal = null,
                    ),
                utfall =
                    SykmeldingDocumentRuleResult(
                        result = RuleType.OK,
                        melding = null,
                    ),
            )
        every { sykmeldingPersistenceService.mapDatabaseEntityToSykmeldingDocument(any()) } returns
            sykmeldingDocument

        every {
            sykmeldingPersistenceService.saveSykmeldingPayload(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            sykmeldingPersistenceService.mapDatabaseEntityToSykmeldingDocument(
                SykmeldingDb(
                    sykmeldingId = sykmeldingId,
                    mottatt = OffsetDateTime.now(),
                    pasientIdent = pasientIdent,
                    sykmelderHpr = behandlerHpr,
                    legekontorOrgnr = "987654321",
                    sykmelding = getTestSykmelding(),
                    legekontorTlf = "12345678",
                    fom = LocalDate.parse("2020-01-01"),
                    tom = LocalDate.parse("2020-01-30"),
                    idempotencyKey = UUID.randomUUID(),
                ),
            )

        every {
            sykmeldingInputProducer.send(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs

        val result =
            sykmeldingService.createSykmelding(
                payload =
                    OpprettSykmeldingPayload(
                        submitId = UUID.randomUUID(),
                        meta =
                            OpprettSykmeldingMetadata(
                                pasientIdent = pasientIdent,
                                sykmelderHpr = "123456789",
                                legekontorOrgnr = "987654321",
                                legekontorTlf = "577788888",
                                source = "Source (FHIR)",
                            ),
                        values =
                            OpprettSykmelding(
                                hoveddiagnose =
                                    OpprettSykmeldingDiagnoseInfo(
                                        system = DiagnoseSystem.ICD10,
                                        code = "S017",
                                    ),
                                aktivitet =
                                    listOf(
                                        OpprettSykmeldingAktivitet.IkkeMulig(
                                            fom = LocalDate.parse("2020-01-01"),
                                            tom = LocalDate.parse("2020-01-30"),
                                            medisinskArsak =
                                                OpprettSykmeldingMedisinskArsak(
                                                    isMedisinskArsak = true
                                                ),
                                            arbeidsrelatertArsak =
                                                OpprettSykmeldingArbeidsrelatertArsak(
                                                    isArbeidsrelatertArsak = false,
                                                    arbeidsrelaterteArsaker = emptyList(),
                                                    annenArbeidsrelatertArsak = null
                                                )
                                        ),
                                    ),
                                pasientenSkalSkjermes = false,
                                bidiagnoser = emptyList(),
                                meldinger =
                                    OpprettSykmeldingMeldinger(
                                        tilNav = null,
                                        tilArbeidsgiver = null,
                                    ),
                                svangerskapsrelatert = false,
                                yrkesskade = null,
                                arbeidsgiver = null,
                                tilbakedatering = null,
                                utdypendeSporsmal = null
                            ),
                    ),
            )

        verify(exactly = 1) {
            sykmeldingInputProducer.send(any(), any(), any(), any(), any(), any())
        }

        result.fold({ fail("Expected success but got failure: $it") }) { assertNotNull(it) }
    }

    @Test
    fun `successfully creates sykmelding even with rule tree hit`() {
        every { sykmelderService.sykmelderMedSuspensjon(behandlerHpr, any(), any()) } returns
            Result.success(
                Sykmelder.MedSuspensjon(
                    hpr = behandlerHpr,
                    ident = "12345678901",
                    navn =
                        Navn(
                            fornavn = "Ola",
                            mellomnavn = null,
                            etternavn = "Nordmann",
                        ),
                    godkjenninger =
                        listOf(
                            HprGodkjenning(
                                helsepersonellkategori =
                                    HprKode(
                                        aktiv = true,
                                        oid = 0,
                                        verdi = "LE",
                                    ),
                                autorisasjon =
                                    HprKode(
                                        aktiv = true,
                                        oid = 7704,
                                        verdi = "1",
                                    ),
                                tillegskompetanse = null,
                            ),
                        ),
                    suspendert = false,
                ),
            )

        every { personService.getPersonByIdent(any()) } returns
            Result.success(Person(navn = navn, fodselsdato = foedselsdato, ident = "123"))

        every { ruleService.validateRules(any(), any(), any(), foedselsdato) } returns
            RegulaResult.NotOk(
                status = RegulaStatus.INVALID,
                outcome =
                    RegulaOutcome(
                        tree = "Test tree",
                        status = RegulaOutcomeStatus.INVALID,
                        rule = "the rule that failed",
                        reason = RegulaOutcomeReason("validation failed", "message for sender"),
                    ),
                results = emptyList(),
            )

        val sykmeldingDocument =
            SykmeldingDocument(
                sykmeldingId = sykmeldingId,
                meta =
                    SykmeldingDocumentMeta(
                        mottatt = OffsetDateTime.now(),
                        pasientIdent = pasientIdent,
                        sykmelder =
                            SykmeldingDocumentSykmelder(
                                hprNummer = behandlerHpr,
                                fornavn = "Magnar",
                                mellomnavn = null,
                                etternavn = "Koman"
                            ),
                        legekontorOrgnr = "987654321",
                        legekontorTlf = "12345678",
                    ),
                values =
                    SykmeldingDocumentValues(
                        hoveddiagnose =
                            SykmeldingDocumentDiagnoseInfo(
                                system = DiagnoseSystem.ICD10,
                                code = "Z01",
                                text = "Ukjent diagnose",
                            ),
                        aktivitet =
                            listOf(
                                SykmeldingDocumentAktivitet.IkkeMulig(
                                    fom = LocalDate.parse("2020-01-01"),
                                    tom = LocalDate.parse("2020-01-30"),
                                    medisinskArsak =
                                        SykmeldingDocumentMedisinskArsak(isMedisinskArsak = true),
                                    arbeidsrelatertArsak =
                                        SykmeldingDocumentArbeidsrelatertArsak(
                                            isArbeidsrelatertArsak = false,
                                            arbeidsrelaterteArsaker = emptyList(),
                                            annenArbeidsrelatertArsak = null
                                        )
                                ),
                            ),
                        bidiagnoser = emptyList(),
                        svangerskapsrelatert = false,
                        pasientenSkalSkjermes = false,
                        meldinger =
                            SykmeldingDocumentMeldinger(
                                tilNav = null,
                                tilArbeidsgiver = null,
                            ),
                        yrkesskade = null,
                        arbeidsgiver = null,
                        tilbakedatering = null,
                        utdypendeSporsmal = null,
                    ),
                utfall =
                    SykmeldingDocumentRuleResult(
                        result = RuleType.OK,
                        melding = null,
                    ),
            )
        every { sykmeldingPersistenceService.mapDatabaseEntityToSykmeldingDocument(any()) } returns
            sykmeldingDocument

        every {
            sykmeldingPersistenceService.saveSykmeldingPayload(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } returns
            sykmeldingPersistenceService.mapDatabaseEntityToSykmeldingDocument(
                SykmeldingDb(
                    sykmeldingId = sykmeldingId,
                    mottatt = OffsetDateTime.now(),
                    pasientIdent = pasientIdent,
                    sykmelderHpr = behandlerHpr,
                    legekontorOrgnr = "987654321",
                    sykmelding = getTestSykmelding(),
                    legekontorTlf = "12345678",
                    fom = LocalDate.parse("2020-01-01"),
                    tom = LocalDate.parse("2020-01-30"),
                    idempotencyKey = UUID.randomUUID(),
                ),
            )

        every {
            sykmeldingInputProducer.send(
                any(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        } just Runs

        val result =
            sykmeldingService.createSykmelding(
                payload =
                    OpprettSykmeldingPayload(
                        submitId = UUID.randomUUID(),
                        meta =
                            OpprettSykmeldingMetadata(
                                pasientIdent = "12345678901",
                                sykmelderHpr = "123456789",
                                legekontorOrgnr = "987654321",
                                legekontorTlf = "12345678",
                                source = "Source (FHIR)",
                            ),
                        values =
                            OpprettSykmelding(
                                hoveddiagnose =
                                    OpprettSykmeldingDiagnoseInfo(
                                        system = DiagnoseSystem.ICD10,
                                        code = "Z01",
                                    ),
                                aktivitet =
                                    listOf(
                                        OpprettSykmeldingAktivitet.IkkeMulig(
                                            fom = LocalDate.parse("2020-01-01"),
                                            tom = LocalDate.parse("2020-01-30"),
                                            medisinskArsak =
                                                OpprettSykmeldingMedisinskArsak(
                                                    isMedisinskArsak = true
                                                ),
                                            arbeidsrelatertArsak =
                                                OpprettSykmeldingArbeidsrelatertArsak(
                                                    isArbeidsrelatertArsak = false,
                                                    arbeidsrelaterteArsaker = emptyList(),
                                                    annenArbeidsrelatertArsak = null
                                                )
                                        ),
                                    ),
                                pasientenSkalSkjermes = false,
                                bidiagnoser = emptyList(),
                                meldinger =
                                    OpprettSykmeldingMeldinger(
                                        tilNav = null,
                                        tilArbeidsgiver = null,
                                    ),
                                svangerskapsrelatert = false,
                                yrkesskade = null,
                                arbeidsgiver = null,
                                tilbakedatering = null,
                                utdypendeSporsmal = null
                            ),
                    ),
            )

        result.fold({ fail("Expected success but got failure: $it") }) { assertNotNull(it) }
    }

    private fun getTestSykmelding(): PersistedSykmelding {
        return PersistedSykmelding(
            hoveddiagnose =
                PersistedSykmeldingDiagnoseInfo(
                    system = DiagnoseSystem.ICD10,
                    code = "Z01",
                    text = "Angst"
                ),
            aktivitet =
                listOf(
                    PersistedSykmeldingAktivitet.IkkeMulig(
                        fom = LocalDate.parse("2020-01-01"),
                        tom = LocalDate.parse("2020-01-30"),
                        medisinskArsak = PersistedSykmeldingMedisinskArsak(isMedisinskArsak = true),
                        arbeidsrelatertArsak =
                            PersistedSykmeldingArbeidsrelatertArsak(
                                isArbeidsrelatertArsak = false,
                                arbeidsrelaterteArsaker = emptyList(),
                                annenArbeidsrelatertArsak = null
                            )
                    ),
                ),
            pasientenSkalSkjermes = false,
            bidiagnoser = emptyList(),
            meldinger = PersistedSykmeldingMeldinger(tilNav = null, tilArbeidsgiver = null),
            svangerskapsrelatert = false,
            yrkesskade = null,
            arbeidsgiver = null,
            tilbakedatering = null,
            utdypendeSporsmal = null,
            sykmeldingId = sykmeldingId,
            pasient =
                PersistedSykmeldingPasient(
                    Navn("Ola", "", "Nordmann"),
                    pasientIdent,
                    LocalDate.parse("1970-01-01")
                ),
            sykmelder =
                PersistedSykmeldingSykmelder(
                    ident = behandlerIdent,
                    hprNummer = behandlerHpr,
                    fornavn = "Lege",
                    mellomnavn = "",
                    etternavn = "Legesen"
                ),
            regelResultat =
                PersistedSykmeldingRuleResult(
                    result = RuleType.OK,
                    meldingTilSender = "Dette er en melding"
                ),
        )
    }
}
