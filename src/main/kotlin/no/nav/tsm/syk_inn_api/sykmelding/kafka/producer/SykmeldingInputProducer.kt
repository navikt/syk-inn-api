package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.utils.logger
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SykmeldingInputProducer(
    private val kafkaProducer: KafkaProducer<String, SykmeldingRecord>,
) {
    @Value("\${kafka.topics.sykmeldinger-input}") private lateinit var sykmeldingInputTopic: String

    private val logger = logger()

    fun send(
        sykmeldingId: String,
        sykmelding: SykmeldingDocument,
        person: Person,
        sykmelder: Sykmelder,
        regulaResult: RegulaResult,
    ) {
        val sykmeldingKafkaMessage =
            SykmeldingRecord(
                metadata = SykmeldingKafkaMapper.mapMessageMetadata(sykmelding.meta),
                sykmelding =
                    SykmeldingKafkaMapper.mapToDigitalSykmelding(
                        sykmelding,
                        sykmeldingId,
                        person,
                        sykmelder,
                    ),
                validation = SykmeldingKafkaMapper.mapValidationResult(regulaResult),
            )
        kafkaProducer
            .send(
                ProducerRecord(
                    sykmeldingInputTopic,
                    sykmeldingId,
                    sykmeldingKafkaMessage,
                ),
            )
            .get()

        logger.info("Sent sykmelding with id=$sykmeldingId to Kafka")
    }
}
