package no.nav.tsm.sykinnapi.service.sykmeldingInnsending

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.verify
import java.util.*
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import no.nav.tsm.sykinnapi.service.syfohelsenettproxy.SyfohelsenettproxyService
import no.nav.tsm.sykinnapi.service.syfosmregler.SyfosmreglerService
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingService
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SykmeldingInnsendingTest {

    @MockK lateinit var sykmeldingService: SykmeldingService

    @MockK lateinit var syfohelsenettproxyService: SyfohelsenettproxyService

    @MockK lateinit var syfosmreglerService: SyfosmreglerService

    @InjectMockKs lateinit var receivedSykmeldingMapper: ReceivedSykmeldingMapper

    @MockK lateinit var objectMapper: ObjectMapper
    @InjectMockKs lateinit var sykmeldingInnsending: SykmeldingInnsending

    @Test
    fun test() {

        val fnr = "1344333"
        val id = UUID.randomUUID().toString()
        val hpr = "123123"

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
        every { objectMapper.writeValueAsString(any()) } returns "42"
        every { syfohelsenettproxyService.getBehandlerByHpr(any(), any()) } returns
            Behandler(
                godkjenninger = emptyList(),
                fnr = fnr,
                hprNummer = hpr,
                fornavn = "Fornavn",
                mellomnavn = null,
                etternavn = "etternavn"
            )
        every { syfosmreglerService.validate(any()) } returns ValidationResult.OK
        every { sykmeldingService.sendToOkTopic(any()) } returns id
        sykmeldingInnsending.send(sykInnApiNySykmeldingPayload)
        verify {
            syfohelsenettproxyService.getBehandlerByHpr(any(), any())
            // receivedSykmeldingMapper.mapToReceivedSykmelding(any(), any(), any())
            syfosmreglerService.validate(any())
            // receivedSykmeldingMapper.mapToReceivedSykmeldingWithValidationResult(any(),any())
            sykmeldingService.sendToOkTopic(any())
        }
    }
}
