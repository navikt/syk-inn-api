package no.nav.tsm.syk_inn_api.sykmelding.rules

import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder.MedSuspensjon
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprKode
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingMetadata
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingPayload
import org.junit.jupiter.api.Assertions.*

class RuleServiceTest {

    val ruleService: RuleService = RuleService()

    @Test
    fun `should validate OK`() {
        val result =
            ruleService.validateRules(
                sykmeldingId = "test-foo-bar",
                payload =
                    OpprettSykmeldingPayload(
                        submitId = UUID.randomUUID(),
                        meta = okMetadata,
                        values =
                            OpprettSykmelding(
                                pasientenSkalSkjermes = false,
                                svangerskapsrelatert = false,
                                hoveddiagnose =
                                    OpprettSykmeldingDiagnoseInfo(
                                        code = "A01",
                                        system = DiagnoseSystem.ICPC2,
                                    ),
                                bidiagnoser = emptyList(),
                                aktivitet =
                                    listOf(
                                        OpprettSykmeldingAktivitet.Gradert(
                                            grad = 69,
                                            fom = LocalDate.now().minusDays(3),
                                            tom = LocalDate.now().plusDays(10),
                                            reisetilskudd = false,
                                        ),
                                    ),
                                meldinger =
                                    OpprettSykmeldingMeldinger(
                                        tilNav = null,
                                        tilArbeidsgiver = null,
                                    ),
                                yrkesskade = null,
                                arbeidsgiver = null,
                                tilbakedatering = null,
                                utdypendeSporsmal = null,
                            ),
                    ),
                sykmelder = okSykmelder,
                foedselsdato = LocalDate.now().minusYears(20),
            )

        assertEquals(result.status, RegulaStatus.OK)
    }

    @Test
    fun `should automatically validate OK for ICPC2B for both hoved and bi`() {
        val result =
            ruleService.validateRules(
                sykmeldingId = "test-foo-bar",
                payload =
                    OpprettSykmeldingPayload(
                        submitId = UUID.randomUUID(),
                        meta = okMetadata,
                        values =
                            OpprettSykmelding(
                                pasientenSkalSkjermes = false,
                                svangerskapsrelatert = false,
                                hoveddiagnose =
                                    OpprettSykmeldingDiagnoseInfo(
                                        code = "A01.0001",
                                        system = DiagnoseSystem.ICPC2B,
                                    ),
                                bidiagnoser =
                                    listOf(
                                        OpprettSykmeldingDiagnoseInfo(
                                            code = "Y99.0001",
                                            system = DiagnoseSystem.ICPC2B,
                                        ),
                                    ),
                                aktivitet =
                                    listOf(
                                        OpprettSykmeldingAktivitet.Gradert(
                                            grad = 69,
                                            fom = LocalDate.now().minusDays(3),
                                            tom = LocalDate.now().plusDays(10),
                                            reisetilskudd = false,
                                        ),
                                    ),
                                meldinger =
                                    OpprettSykmeldingMeldinger(
                                        tilNav = null,
                                        tilArbeidsgiver = null,
                                    ),
                                yrkesskade = null,
                                arbeidsgiver = null,
                                tilbakedatering = null,
                                utdypendeSporsmal = null,
                            ),
                    ),
                sykmelder = okSykmelder,
                foedselsdato = LocalDate.now().minusYears(20),
            )

        assertEquals(result.status, RegulaStatus.OK)
    }

    private val okMetadata =
        OpprettSykmeldingMetadata(
            source = "test",
            pasientIdent = "12345678901",
            sykmelderHpr = "123",
            legekontorOrgnr = "000000000",
            legekontorTlf = "00000000",
        )

    private val okSykmelder =
        MedSuspensjon(
            hpr = "123456",
            navn = null,
            ident = "987654321",
            godkjenninger =
                listOf(
                    HprGodkjenning(
                        helsepersonellkategori = HprKode(aktiv = true, oid = 1, verdi = "LE"),
                        autorisasjon = HprKode(aktiv = true, oid = 7704, verdi = "1"),
                        tillegskompetanse = null,
                    ),
                ),
            suspendert = false,
        )
}
