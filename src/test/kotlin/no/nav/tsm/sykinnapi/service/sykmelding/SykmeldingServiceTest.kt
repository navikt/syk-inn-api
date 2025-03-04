package no.nav.tsm.sykinnapi.service.sykmelding

import com.fasterxml.jackson.databind.ObjectMapper
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import no.nav.tsm.sykinnapi.config.kafka.SykmeldingOKProducer
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Status
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.toReceivedSykmeldingWithValidation
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import no.nav.tsm.sykinnapi.service.smpdfgen.SmPdfGenService
import no.nav.tsm.sykinnapi.service.syfohelsenettproxy.SyfohelsenettproxyService
import no.nav.tsm.sykinnapi.service.syfosmregister.SyfosmregisterService
import no.nav.tsm.sykinnapi.service.syfosmregler.SyfosmreglerService
import no.nav.tsm.sykinnapi.service.tsmpdl.TsmPdlService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.json.JsonTest

@JsonTest
class SykmeldingServiceTest {

    @MockK lateinit var sykmeldingOKProducer: SykmeldingOKProducer

    @MockK lateinit var syfosmregisterService: SyfosmregisterService

    @MockK lateinit var syfohelsenettproxyService: SyfohelsenettproxyService

    @MockK lateinit var syfosmreglerService: SyfosmreglerService

    @MockK lateinit var receivedSykmeldingMapper: ReceivedSykmeldingMapper

    lateinit var sykmeldingService: SykmeldingService

    @Autowired private lateinit var objectMapper: ObjectMapper

    @BeforeEach
    fun setup() {
        sykmeldingService =
            SykmeldingService(
                syfosmregisterService = syfosmregisterService,
                sykmeldingOKProducer = sykmeldingOKProducer,
                syfohelsenettproxyService = syfohelsenettproxyService,
                syfosmreglerService = syfosmreglerService,
                receivedSykmeldingMapper = receivedSykmeldingMapper,
                objectMapper = objectMapper,
            )
    }

    @Test
    internal fun `Should return not throw exception when valid payload`() {

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
                    ),
                )

        assertDoesNotThrow { sykmeldingService.sendToOkTopic(receivedSykmeldingWithValidation) }
    }

    @Test
    fun shouldSendSykmeldingAndTriggerValidationAndProcessing() {

        val fnr = "1344333"
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
        every { syfohelsenettproxyService.getBehandlerByHpr(any(), any()) } returns
            Behandler(
                godkjenninger = emptyList(),
                fnr = fnr,
                hprNummer = hpr,
                fornavn = "Fornavn",
                mellomnavn = null,
                etternavn = "etternavn",
            )
        every { syfosmreglerService.validate(any()) } returns ValidationResult.OK
        every { sykmeldingService.sendToOkTopic(any()) } returns Unit
        every { receivedSykmeldingMapper.mapToReceivedSykmelding(any(), any(), any()) } returns
            mockk()
        every {
            receivedSykmeldingMapper.mapToReceivedSykmeldingWithValidationResult(any(), any())
        } returns mockk()

        sykmeldingService.sendSykmelding(sykInnApiNySykmeldingPayload)
        verify {
            syfohelsenettproxyService.getBehandlerByHpr(any(), any())
            syfosmreglerService.validate(any())
            sykmeldingService.sendToOkTopic(any())
        }
    }
}
