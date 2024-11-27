package no.nav.tsm.sykinnapi.service.sykmelding

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.sykinnapi.config.kafka.OK_SYKMLEDING_TOPIC
import no.nav.tsm.sykinnapi.mapper.receivedSykmeldingWithValidationMapper
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    private val sykmeldingOKProducer: KafkaProducer<String, ReceivedSykmelding>
) {
    private val logger = LoggerFactory.getLogger(SykmeldingService::class.java)

    fun create(
        sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload,
        sykmelderFnr: String,
        sykmeldingId: String
    ): String {

        val receivedSykmeldingWithValidation =
            receivedSykmeldingWithValidationMapper(
                sykInnApiNySykmeldingPayload,
                sykmelderFnr,
                sykmeldingId
            )

        logger.info(
            "Successfully created receivedSykmeldingWithValidation: ${
                ObjectMapper().apply {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                }.writeValueAsString(receivedSykmeldingWithValidation)}"
        )

        // TODO put receivedSykmeldingWithValidationResult on kafka topic
        try {
           sykmeldingOKProducer.send(
                    ProducerRecord(
                        OK_SYKMLEDING_TOPIC,
                        receivedSykmeldingWithValidation.sykmelding.id,
                        receivedSykmeldingWithValidation
                    ),
                ).get()

            logger.info(
                "Sykmelding sendt to kafka topic {} sykmelding id {}",
                OK_SYKMLEDING_TOPIC,
                receivedSykmeldingWithValidation.sykmelding.id,
            )
        } catch (exception: Exception) {
            logger.error(
                "failed to send sykmelding to kafka result for sykmelding {}",
                receivedSykmeldingWithValidation.sykmelding.id
            )
            throw exception
        }

        // TODO Do rule check here, api? regilus-maximus, or call to syfosmregler?

        return receivedSykmeldingWithValidation.sykmelding.id
    }
}
