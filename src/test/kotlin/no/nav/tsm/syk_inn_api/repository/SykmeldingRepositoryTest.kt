package no.nav.tsm.syk_inn_api.repository

import no.nav.tsm.syk_inn_api.model.sykmelding.Aktivitet
import no.nav.tsm.syk_inn_api.model.sykmelding.DiagnoseSystem
import no.nav.tsm.syk_inn_api.model.sykmelding.Hoveddiagnose
import no.nav.tsm.syk_inn_api.model.sykmelding.Sykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingEntity
import org.assertj.core.api.Assertions.assertThat
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.annotation.Transactional
import kotlin.test.Test

@Transactional
class SykmeldingRepositoryTest: IntegrationTest()  {

    @Autowired
    lateinit var sykmeldingRepository: SykmeldingRepository



    @Test
    fun `should save and find sykmelding entity by sykmeldingId`() {
        val sykmeldingEntity = SykmeldingEntity(
            sykmeldingId = "sykmelding-123",
            pasientFnr = "12345678910",
            sykmelderHpr = "123456",
            legekontorOrgnr = "987654321",
            sykmelding = Sykmelding(
                hoveddiagnose = Hoveddiagnose(DiagnoseSystem.ICD10, "R99"),
                aktivitet = Aktivitet.IkkeMulig("2024-04-01", "2024-04-10")
            )
        )


        val savedEntity = sykmeldingRepository.save(sykmeldingEntity)

        val found = sykmeldingRepository.findSykmeldingEntityBySykmeldingId("sykmelding-123")

        assertThat(found).isNotNull
        assertThat(found?.id).isEqualTo(savedEntity.id)
        assertThat(found?.pasientFnr).isEqualTo("12345678910")
    }

}
