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
import no.nav.tsm.syk_inn_api.sykmelder.btsys.BtsysService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HelsenettProxyService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprKode
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.kafka.SykmeldingKafkaService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.toPGobject
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeta
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentValues
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SykmeldingServiceTest {
    private lateinit var sykmeldingService: SykmeldingService
    private lateinit var helsenettProxyService: HelsenettProxyService
    private lateinit var ruleService: RuleService
    private lateinit var sykmeldingKafkaService: SykmeldingKafkaService
    private lateinit var sykmeldingPersistenceService: SykmeldingPersistenceService
    private lateinit var btsysService: BtsysService
    private lateinit var personService: PersonService

    val behandlerHpr = "123456789"
    val sykmeldingId = UUID.randomUUID().toString()
    val foedselsdato = LocalDate.of(1990, 1, 1)
    val navn = Navn(fornavn = "Ola", mellomnavn = null, etternavn = "Nordmann")

    @BeforeEach
    fun setup() {
        helsenettProxyService = mockk()
        ruleService = mockk()
        sykmeldingPersistenceService = mockk()
        sykmeldingKafkaService = mockk()
        personService = mockk()
        btsysService = mockk()
        sykmeldingService =
            SykmeldingService(
                sykmeldingPersistenceService = sykmeldingPersistenceService,
                ruleService = ruleService,
                helsenettProxyService = helsenettProxyService,
                btsysService = btsysService,
                sykmeldingKafkaService = sykmeldingKafkaService,
                personService = personService,
            )
    }

    @Test
    fun `create sykmelding with valid data`() {
        every { helsenettProxyService.getSykmelderByHpr(behandlerHpr, any()) } returns
            Result.success(
                HprSykmelder(
                    hprNummer = behandlerHpr,
                    fnr = "01019078901",
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann",
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
                ),
            )

        every { personService.getPersonByIdent(any()) } returns
            Result.success(Person(navn = navn, fodselsdato = foedselsdato, ident = "123"))
        every { ruleService.validateRules(any(), any(), any(), any(), foedselsdato) } returns
            RegulaResult.Ok(
                emptyList(),
            )

        every { btsysService.isSuspended(any(), any()) } returns Result.success(false)

        val sykmeldingDocument =
            SykmeldingDocument(
                sykmeldingId = sykmeldingId,
                meta =
                    SykmeldingDocumentMeta(
                        mottatt = OffsetDateTime.now(),
                        pasientIdent = "01019078901",
                        sykmelderHpr = behandlerHpr,
                        legekontorOrgnr = "987654321",
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
                                    fom = "2020-01-01",
                                    tom = "2020-01-30",
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
                        regelResultat =
                            SykmeldingDocumentRuleResult(
                                result = "OK",
                                melding = null,
                            ),
                    ),
            )
        every { sykmeldingPersistenceService.mapDatabaseEntityToSykmeldingDocumentt(any()) } returns
            sykmeldingDocument

        every {
            sykmeldingPersistenceService.saveSykmeldingPayload(
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        } returns
            sykmeldingPersistenceService.mapDatabaseEntityToSykmeldingDocumentt(
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

        every { sykmeldingKafkaService.send(any(), any(), any(), any(), any()) } just Runs

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
                                            fom = "2020-01-01",
                                            tom = "2020-01-30",
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
        every { helsenettProxyService.getSykmelderByHpr(behandlerHpr, any()) } returns
            Result.success(
                HprSykmelder(
                    hprNummer = behandlerHpr,
                    fnr = "12345678901",
                    fornavn = "Ola",
                    mellomnavn = null,
                    etternavn = "Nordmann",
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
                ),
            )

        every { personService.getPersonByIdent(any()) } returns
            Result.success(Person(navn = navn, fodselsdato = foedselsdato, ident = "123"))

        every { ruleService.validateRules(any(), any(), any(), any(), foedselsdato) } returns
            RegulaResult.NotOk(
                status = RegulaStatus.INVALID,
                outcome =
                    RegulaOutcome(
                        status = RegulaOutcomeStatus.INVALID,
                        rule = "the rule that failed",
                        reason = RegulaOutcomeReason("validation failed", "message for sender"),
                    ),
                results = emptyList(),
            )

        every { btsysService.isSuspended(any(), any()) } returns Result.success(false)

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
                                            fom = "2020-01-01",
                                            tom = "2020-01-30",
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
            { assertEquals(it, SykmeldingService.SykmeldingCreationErrors.RULE_VALIDATION) },
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
                        fom = "2020-01-01",
                        tom = "2020-01-30",
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
