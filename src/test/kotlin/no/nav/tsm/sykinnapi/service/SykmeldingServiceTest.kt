package no.nav.tsm.sykinnapi.service

import kotlin.test.assertEquals
import no.nav.tsm.sykinnapi.modell.Aktivitet
import no.nav.tsm.sykinnapi.modell.AktivitetType
import no.nav.tsm.sykinnapi.modell.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.Sykmelding
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.any
import org.mockito.InjectMocks
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension

@ExtendWith(MockitoExtension::class)
class SykmeldingServiceTest {

    @InjectMocks lateinit var sykmeldingService: SykmeldingService

    @Test
    internal fun `Should return correct`() {

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
                            Aktivitet(
                                type = AktivitetType.AKTIVITET_IKKE_MULIG,
                            ),
                    ),
            )

        val isCreated = sykmeldingService.create(sykInnApiNySykmeldingPayload)
        assertEquals(true, isCreated)
    }
}
