package no.nav.tsm.sykinnapi.service.sykmelding

import no.nav.tsm.sykinnapi.config.kafka.OK_SYKMLEDING_TOPIC
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmeldingWithValidationResult
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    private val sykmeldingOKProducer: KafkaProducer<String, ReceivedSykmeldingWithValidationResult>
) {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    fun sendToOkTopic(
        receivedSykmeldingWithValidationResult: ReceivedSykmeldingWithValidationResult,
    ): String {

        try {
            sykmeldingOKProducer
                .send(
                    ProducerRecord(
                        OK_SYKMLEDING_TOPIC,
                        receivedSykmeldingWithValidationResult.sykmelding.id,
                        receivedSykmeldingWithValidationResult
                    ),
                )
                .get()

            logger.info(
                "Sykmelding sendt to kafka topic {} sykmelding id {}",
                OK_SYKMLEDING_TOPIC,
                receivedSykmeldingWithValidationResult.sykmelding.id,
            )
        } catch (exception: Exception) {
            logger.error(
                "failed to send sykmelding to kafka result for sykmelding {}",
                receivedSykmeldingWithValidationResult.sykmelding.id
            )
            throw exception
        }

        return receivedSykmeldingWithValidationResult.sykmelding.id
    }
}
