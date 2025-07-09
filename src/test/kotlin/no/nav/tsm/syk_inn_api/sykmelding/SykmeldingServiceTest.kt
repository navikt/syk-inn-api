package no.nav.tsm.syk_inn_api.sykmelding

import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.fail
import no.nav.tsm.regulus.regula.RegulaOutcome
import no.nav.tsm.regulus.regula.RegulaOutcomeReason
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.Navn
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprKode
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingProducer
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.toPGobject
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

    val behandlerHpr = "123456789"
    val sykmeldingId = UUID.randomUUID().toString()
    val foedselsdato = LocalDate.of(1990, 1, 1)
    val navn = Navn(fornavn = "Ola", mellomnavn = null, etternavn = "Nordmann")

    @BeforeEach
    fun setup() {
        ruleService = mockk()
        sykmeldingPersistenceService = mockk()
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
                    ident = "01019078901",
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
                        pasientIdent = "01019078901",
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
                    ),
                utfall =
                    SykmeldingDocumentRuleResult(
                        result = "OK",
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
                    pasientIdent = "01019078901",
                    sykmelderHpr = behandlerHpr,
                    legekontorOrgnr = "987654321",
                    sykmelding = getTestSykmelding().toPGobject(),
                    legekontorTlf = "12345678",
                ),
            )

        every { sykmeldingInputProducer.send(any(), any(), any(), any(), any()) } just Runs

        val result =
            sykmeldingService.createSykmelding(
                payload =
                    OpprettSykmeldingPayload(
                        meta =
                            OpprettSykmeldingMetadata(
                                pasientIdent = "01019078901",
                                sykmelderHpr = "123456789",
                                legekontorOrgnr = "987654321",
                                legekontorTlf = "577788888",
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
                            ),
                    ),
            )

        result.fold({ fail("Expected success but got failure: $it") }) { assertNotNull(it) }
    }

    @Test
    fun `failing to create sykmelding because of rule tree hit`() {
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

        val result =
            sykmeldingService.createSykmelding(
                payload =
                    OpprettSykmeldingPayload(
                        meta =
                            OpprettSykmeldingMetadata(
                                pasientIdent = "12345678901",
                                sykmelderHpr = "123456789",
                                legekontorOrgnr = "987654321",
                                legekontorTlf = "12345678",
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
                            ),
                    ),
            )

        result.fold(
            {
                assertIs<SykmeldingService.SykmeldingCreationErrors.RuleValidation>(it)
                assertEquals(it.result.outcome.rule, "the rule that failed")
            },
        ) {
            fail("Expected rule validation error but got success: $it")
        }
    }

    private fun getTestSykmelding(): OpprettSykmelding {
        return OpprettSykmelding(
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
                        medisinskArsak = OpprettSykmeldingMedisinskArsak(isMedisinskArsak = true),
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
            meldinger = OpprettSykmeldingMeldinger(tilNav = null, tilArbeidsgiver = null),
            svangerskapsrelatert = false,
            yrkesskade = null,
            arbeidsgiver = null,
            tilbakedatering = null,
        )
    }
}
