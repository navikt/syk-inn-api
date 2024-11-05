package no.nav.tsm.sykinnapi.controllers

import no.nav.tsm.sykinnapi.modell.Aktivitet
import no.nav.tsm.sykinnapi.modell.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.Sykmelding
import no.nav.tsm.sykinnapi.service.SykmeldingService
import org.hamcrest.CoreMatchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@WebMvcTest(SykmeldingApiController::class)
class SykmeldingApiControllerTest(
    @Autowired val mockMvc: MockMvc,
    @MockBean val sykmeldingService: SykmeldingService
) {

    @Test
    internal fun `Should return HttpStatus OK and body text ok`() {
        `when`(sykmeldingService.create(any())).thenReturn(true)
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

        mockMvc
            .perform(
                MockMvcRequestBuilders.post(
                    "/api/v1/sykmelding/create",
                    sykInnApiNySykmeldingPayload,
                ),
            )
            .andExpect(status().isOk())
            .andExpect(content().string(containsString("ok")))
    }
}
