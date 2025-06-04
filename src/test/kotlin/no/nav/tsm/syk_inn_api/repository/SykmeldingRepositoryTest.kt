package no.nav.tsm.syk_inn_api.repository

import java.time.LocalDate
import kotlin.test.Test
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.Navn
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingPasient
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingRepository
import no.nav.tsm.syk_inn_api.sykmelding.persistence.toPGobject
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional

@Transactional
class SykmeldingRepositoryTest : IntegrationTest() {

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
                sykmelding =
                    PersistedSykmelding(
                            hoveddiagnose =
                                PersistedSykmeldingDiagnoseInfo(
                                    DiagnoseSystem.ICD10,
                                    "R99",
                                    "Ukjent diagnose"
                                ),
                            aktivitet =
                                listOf(
                                    PersistedSykmeldingAktivitet.IkkeMulig(
                                        "2024-04-01",
                                        "2024-04-10"
                                    )
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
                                            etternavn = "Nordmann"
                                        ),
                                    ident = pasientIdent,
                                    fodselsdato = LocalDate.of(1990, 1, 1)
                                ),
                            sykmelder =
                                PersistedSykmeldingSykmelder(
                                    godkjenninger = emptyList(),
                                    ident = "02029212345",
                                    hprNummer = sykmelderHpr,
                                    fornavn = "Nicky",
                                    mellomnavn = "D",
                                    etternavn = "Angel"
                                ),
                            tilbakedatering = null,
                            regelResultat =
                                PersistedSykmeldingRuleResult(
                                    result = "OK",
                                    meldingTilSender = null,
                                ),
                        )
                        .toPGobject(),
                id = null,
                legekontorTlf = "12345678",
                validertOk = false,
            )

        val savedEntity = sykmeldingRepository.save(sykmeldingDb)

        val found = sykmeldingRepository.findSykmeldingEntityBySykmeldingId("sykmelding-123")

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(savedEntity.id)
        assertThat(found?.pasientIdent).isEqualTo(pasientIdent)
    }
}
