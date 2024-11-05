package no.nav.tsm.sykinnapi.service

import kotlin.test.assertEquals
import no.nav.tsm.sykinnapi.modell.Aktivitet
import no.nav.tsm.sykinnapi.modell.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.Sykmelding
import org.junit.jupiter.api.Test
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean

@SpringBootTest
class SykmeldingServiceTest {

    @MockBean
    lateinit var sykmeldingService: SykmeldingService

    @Test
    internal fun `Should return isCreated true`() {

        val sykInnApiNySykmeldingPayload =
            SykInnApiNySykmeldingPayload(
                pasientFnr = "12345",
                sykmelderHpr = "123123",
                sykmelding =
                Sykmelding(
                        hoveddiagnose =
                        Hoveddiagnose(
                                system = DiagnoseSystem.ICD10,
                                code = "S017",
                        ),
                        aktivitet =
                        Aktivitet.AktivitetIkkeMulig(
                                fom = "2020-01-01",
                                tom = "2020-01-02",
                        ),
                ),
            )

        `when`(sykmeldingService.create(sykInnApiNySykmeldingPayload)).thenReturn(true)


        val isCreated = sykmeldingService.create(sykInnApiNySykmeldingPayload)
        assertEquals(true, isCreated)
    }
}
