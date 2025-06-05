package no.nav.tsm.syk_inn_api.repository

import kotlin.test.Test
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingDiagnoseInfo
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
        val sykmeldingDb =
            SykmeldingDb(
                sykmeldingId = "sykmelding-123",
                pasientIdent = "12345678910",
                sykmelderHpr = "123456",
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
                                PersistedSykmeldingAktivitet.IkkeMulig("2024-04-01", "2024-04-10")
                        )
                        .toPGobject()
            )

        val savedEntity = sykmeldingRepository.save(sykmeldingDb)

        val found = sykmeldingRepository.findSykmeldingEntityBySykmeldingId("sykmelding-123")

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(savedEntity.id)
        assertThat(found?.pasientIdent).isEqualTo("12345678910")
    }
}
