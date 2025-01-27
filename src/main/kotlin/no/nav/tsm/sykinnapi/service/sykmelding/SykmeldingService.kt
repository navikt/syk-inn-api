package no.nav.tsm.sykinnapi.service.sykmelding

import no.nav.tsm.sykinnapi.config.kafka.SykmeldingOKProducer
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmeldingWithValidationResult
import org.springframework.stereotype.Service

@Service
class SykmeldingService(private val sykmeldingOKProducer: SykmeldingOKProducer) {

    fun sendToOkTopic(
        receivedSykmeldingWithValidationResult: ReceivedSykmeldingWithValidationResult,
    ) {
        sykmeldingOKProducer.send(receivedSykmeldingWithValidationResult)
    }
}
