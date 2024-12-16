package no.nav.tsm.sykinnapi.service.sykmelding

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlin.test.assertTrue
import no.nav.tsm.sykinnapi.config.kafka.SykmeldingOKProducer
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Status
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.toReceivedSykmeldingWithValidation
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest

@RestClientTest
class SykmeldingServiceTest {

    @MockK lateinit var sykmeldingOKProducer: SykmeldingOKProducer

    lateinit var sykmeldingService: SykmeldingService

    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        sykmeldingService = SykmeldingService(sykmeldingOKProducer)
    }

    @Test
    internal fun `Should return sykmeldingId true when valid payload`() {

        val sykmelderFnr = "1344333"

        val sykmeldingsId = "123213-2323-213123123"

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

        every { sykmeldingOKProducer.send(any()) } returns Unit

        val receivedSykmeldingWithValidation =
            ReceivedSykmeldingMapper(objectMapper)
                .mapToReceivedSykmelding(sykInnApiNySykmeldingPayload, sykmelderFnr, sykmeldingsId)
                .toReceivedSykmeldingWithValidation(
                    ValidationResult(
                        Status.OK,
                        emptyList(),
                    )
                )

        val sykmeldingId = sykmeldingService.sendToOkTopic(receivedSykmeldingWithValidation)

        assertTrue(sykmeldingId.isNotBlank())
    }
}
