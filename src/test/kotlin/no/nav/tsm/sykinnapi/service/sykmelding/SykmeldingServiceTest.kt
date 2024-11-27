package no.nav.tsm.sykinnapi.service.sykmelding

import java.util.concurrent.Future
import kotlin.test.assertTrue
import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.sykinn.Aktivitet
import no.nav.tsm.sykinnapi.modell.sykinn.DiagnoseSystem
import no.nav.tsm.sykinnapi.modell.sykinn.Hoveddiagnose
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.Sykmelding
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.clients.producer.RecordMetadata
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.ArgumentCaptor.captor
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.bean.override.mockito.MockitoBean
import kotlin.String

@EnableJwtTokenValidation
@EnableMockOAuth2Server
@SpringBootTest
class SykmeldingServiceTest {

    @MockitoBean lateinit var sykmeldingOKProducer: KafkaProducer<String, ReceivedSykmelding>

    lateinit var sykmeldingService: SykmeldingService

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

        val producerRecord = mock<ProducerRecord<String, ReceivedSykmelding>>()

        val futureRecordMetadata = mock<Future<RecordMetadata>>()
        val captor = captor<ProducerRecord<String, ReceivedSykmelding>>()

        `when`(futureRecordMetadata.get()).thenReturn(mock<RecordMetadata>())
        `when`(sykmeldingOKProducer.send(captor.capture())).thenReturn(futureRecordMetadata)

        `when`(
                sykmeldingOKProducer.send(
                    producerRecord
                )
            )
            .thenReturn(futureRecordMetadata)

        val sykmeldingId =
            sykmeldingService.create(sykInnApiNySykmeldingPayload, sykmelderFnr, sykmeldingsId)

        assertTrue(sykmeldingId.isNotBlank())
    }
}
