package no.nav.tsm.syk_inn_api.sykmelding

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
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
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedRuleType
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingArbeidsrelatertArsak
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMedisinskArsak
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingPasient
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedValidationResult
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykInnPersistence
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SykmeldingServiceTest {
    private lateinit var sykmeldingService: SykmeldingService
    private lateinit var sykmelderService: SykmelderService
    private lateinit var ruleService: RuleService
    private lateinit var sykInnPersistence: SykInnPersistence
    private lateinit var personService: PersonService

    val pasientIdent = "01019078901"
    val behandlerIdent = "0101197054322"
    val behandlerHpr = "123456789"
    val sykmeldingId = UUID.randomUUID().toString()
    val foedselsdato = LocalDate.of(1990, 1, 1)
    val navn = Navn(fornavn = "Ola", mellomnavn = null, etternavn = "Nordmann")

    @BeforeEach
    fun setup() {
        ruleService = mockk()
        sykInnPersistence =
            mockk<SykInnPersistence>().also {
                every { it.getSykmeldingByIdempotencyKey(any()) } returns null
            }
        personService = mockk()
        sykmelderService = mockk()
        sykmeldingService =
            SykmeldingService(
                sykInnPersistence = sykInnPersistence,
                ruleService = ruleService,
                sykmelderService = sykmelderService,
                personService = personService,
            )
    }

    @Test
    fun `create sykmelding with valid data`() {
        val idempotencyKey = UUID.randomUUID()

        every { sykmelderService.sykmelderMedSuspensjon(behandlerHpr, any(), any()) } returns
            Result.success(
                Sykmelder.MedSuspensjon(
                    hpr = behandlerHpr,
                    ident = pasientIdent,
                    navn = navn,
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
            Result.success(Person(navn = navn, fodselsdato = foedselsdato, ident = pasientIdent))
        every { ruleService.validateRules(any(), any(), any(), foedselsdato) } returns
            RegulaResult.Ok(
                emptyList(),
            )

        every { sykInnPersistence.saveSykInnSykmelding(any(), null, any()) } returns
            SykmeldingDb(
                sykmeldingId = sykmeldingId,
                idempotencyKey = idempotencyKey,
                mottatt = OffsetDateTime.now(),
                pasientIdent = pasientIdent,
                sykmelderHpr = behandlerHpr,
                legekontorOrgnr = "987654321",
                sykmelding = getTestSykmelding(),
                legekontorTlf = "12345678",
                fom = LocalDate.parse("2020-01-01"),
                tom = LocalDate.parse("2020-01-30"),
                validationResult =
                    PersistedValidationResult(
                        PersistedRuleType.OK,
                        OffsetDateTime.now(),
                        emptyList()
                    ),
            )

        val result =
            sykmeldingService.createSykmelding(
                payload =
                    OpprettSykmeldingPayload(
                        submitId = idempotencyKey,
                        meta =
                            OpprettSykmeldingMetadata(
                                pasientIdent = pasientIdent,
                                sykmelderHpr = behandlerHpr,
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

        result.fold({ fail("Expected success but got failure: $it") }) { assertNotNull(it) }
    }

    @Test
    fun `successfully creates sykmelding even with rule tree hit`() {
        val idempotencyKey = UUID.randomUUID()

        every { sykmelderService.sykmelderMedSuspensjon(behandlerHpr, any(), any()) } returns
            Result.success(
                Sykmelder.MedSuspensjon(
                    hpr = behandlerHpr,
                    ident = behandlerIdent,
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
            Result.success(Person(navn = navn, fodselsdato = foedselsdato, ident = pasientIdent))

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

        val payload = createSykmeldingPayload(idempotencyKey)
        every { sykInnPersistence.saveSykInnSykmelding(any(), any(), any()) } returns
            mapSykmeldingPayloadToDatabaseEntity(
                sykmeldingId = sykmeldingId,
                mottatt = OffsetDateTime.now(),
                payload = payload,
                pasient = Person(navn = navn, fodselsdato = foedselsdato, ident = pasientIdent),
                sykmelder =
                    Sykmelder.Enkel(payload.meta.sykmelderHpr, null, behandlerIdent, emptyList()),
                ruleResult = mockk(relaxed = true),
            )

        val result =
            sykmeldingService.createSykmelding(
                payload = createSykmeldingPayload(idempotencyKey),
            )

        result.fold({ fail("Expected success but got failure: $it") }) { assertNotNull(it) }
    }
}

fun createSykmeldingPayload(idempotencyKey: UUID = UUID.randomUUID()): OpprettSykmeldingPayload =
    OpprettSykmeldingPayload(
        submitId = idempotencyKey,
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
                                    isMedisinskArsak = true,
                                ),
                            arbeidsrelatertArsak =
                                OpprettSykmeldingArbeidsrelatertArsak(
                                    isArbeidsrelatertArsak = false,
                                    arbeidsrelaterteArsaker = emptyList(),
                                    annenArbeidsrelatertArsak = null,
                                ),
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
                utdypendeSporsmal = null,
            ),
    )

fun getTestSykmelding(
    sykmeldingId: UUID = UUID.randomUUID(),
    pasientIdent: String = "12345678912",
    behandlerIdent: String = "12345678901",
    behandlerHpr: String = "123456789"
): PersistedSykmelding {
    return PersistedSykmelding(
        hoveddiagnose =
            PersistedSykmeldingDiagnoseInfo(
                system = DiagnoseSystem.ICD10,
                code = "Z01",
                text = "Angst",
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
                            annenArbeidsrelatertArsak = null,
                        ),
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
        sykmeldingId = sykmeldingId.toString(),
        pasient =
            PersistedSykmeldingPasient(
                Navn("Ola", "", "Nordmann"),
                pasientIdent,
                LocalDate.parse("1970-01-01"),
            ),
        sykmelder =
            PersistedSykmeldingSykmelder(
                ident = behandlerIdent,
                hprNummer = behandlerHpr,
                fornavn = "Lege",
                mellomnavn = "",
                etternavn = "Legesen",
            ),
        regelResultat =
            PersistedSykmeldingRuleResult(
                result = PersistedRuleType.OK,
                meldingTilSender = "Dette er en melding",
            ),
    )
}
