package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.test.Test
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.person.Navn
import no.nav.tsm.syk_inn_api.test.FullIntegrationTest
import no.nav.tsm.sykmelding.input.core.model.RuleType
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.transaction.annotation.Transactional

@Transactional
@DataJpaTest
class SykmeldingRepositoryTest : FullIntegrationTest() {

    @Autowired lateinit var sykmeldingRepository: SykmeldingRepository

    @Test
    fun `should save and find sykmelding entity by sykmeldingId`() {
        val sykmeldingId = "sykmelding-123"
        val pasientIdent = "010190567891"
        val sykmelderHpr = "123456"
        val sykmeldingDb =
            SykmeldingDb(
                sykmeldingId = sykmeldingId,
                pasientIdent = pasientIdent,
                sykmelderHpr = sykmelderHpr,
                legekontorOrgnr = "987654321",
                mottatt = OffsetDateTime.now(),
                sykmelding =
                    PersistedSykmelding(
                        hoveddiagnose =
                            PersistedSykmeldingDiagnoseInfo(
                                DiagnoseSystem.ICD10,
                                "R99",
                                "Ukjent diagnose",
                            ),
                        aktivitet =
                            listOf(
                                PersistedSykmeldingAktivitet.IkkeMulig(
                                    LocalDate.parse("2024-04-01"),
                                    LocalDate.parse("2024-04-10"),
                                    medisinskArsak =
                                        PersistedSykmeldingMedisinskArsak(isMedisinskArsak = true),
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
                        meldinger =
                            PersistedSykmeldingMeldinger(
                                tilNav = null,
                                tilArbeidsgiver = null,
                            ),
                        svangerskapsrelatert = false,
                        yrkesskade = null,
                        arbeidsgiver = null,
                        sykmeldingId = sykmeldingId,
                        pasient =
                            PersistedSykmeldingPasient(
                                navn =
                                    Navn(
                                        fornavn = "Ola",
                                        mellomnavn = "Norman",
                                        etternavn = "Nordmann",
                                    ),
                                ident = pasientIdent,
                                fodselsdato = LocalDate.of(1990, 1, 1),
                            ),
                        sykmelder =
                            PersistedSykmeldingSykmelder(
                                godkjenninger = emptyList(),
                                ident = "02029212345",
                                hprNummer = sykmelderHpr,
                                fornavn = "Nicky",
                                mellomnavn = "D",
                                etternavn = "Angel",
                            ),
                        tilbakedatering = null,
                        regelResultat =
                            PersistedSykmeldingRuleResult(
                                result = RuleType.OK,
                                meldingTilSender = null,
                            ),
                    ),
                legekontorTlf = "12345678",
                validertOk = false,
            )

        val savedEntity = sykmeldingRepository.save(sykmeldingDb)

        val found = sykmeldingRepository.findSykmeldingEntityBySykmeldingId("sykmelding-123")

        assertThat(found).isNotNull
        assertThat(found?.sykmeldingId).isEqualTo(savedEntity.sykmeldingId)
        assertThat(found?.pasientIdent).isEqualTo(pasientIdent)
    }
}
