package no.nav.tsm.sykinnapi.config.kafka

import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmeldingWithValidationResult
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingService
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.stereotype.Component

@Component
class SykmeldingOKProducer(
    @Value("\${teamsykmelding.ok.sykmelding.topic}") private val okSykmeldingTopic: String,
    private val kafkaTemplate: KafkaTemplate<String, ReceivedSykmeldingWithValidationResult>
) {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    fun send(receivedSykmeldingWithValidationResult: ReceivedSykmeldingWithValidationResult) {
        kafkaTemplate
            .send(
                okSykmeldingTopic,
                receivedSykmeldingWithValidationResult.sykmelding.id,
                receivedSykmeldingWithValidationResult,
            )
            .exceptionally {
                throw IllegalStateException("Could not send send to topic: $okSykmeldingTopic")
            }
            .thenAccept {
                val metadata = it.recordMetadata
                logger.info(
                    "Message sent OK on Topic=[$okSykmeldingTopic], " +
                        "Key=[${receivedSykmeldingWithValidationResult.sykmelding.id}]," +
                        " Offset=[${metadata.offset()}, Partition=[${metadata.partition()}]"
                )
            }
    }
}
