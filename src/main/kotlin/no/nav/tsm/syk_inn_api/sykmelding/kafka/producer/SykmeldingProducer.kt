package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import org.springframework.stereotype.Component

@Component
class SykmeldingProducer(
    private val kafkaProducer: SykmeldingInputProducer,
) {

    private val logger = logger()

    fun send(
        sykmelding: SykmeldingDb,
        source: String,
    ) {
        val sykmeldingKafkaMessage =
            SykmeldingRecord(
                metadata = SykmeldingKafkaMapper.mapMessageMetadata(sykmelding),
                sykmelding = SykmeldingKafkaMapper.mapToDigitalSykmelding(sykmelding, source),
                validation = SykmeldingKafkaMapper.mapValidationResult(sykmelding.validationResult)
            )
        kafkaProducer.sendSykmelding(sykmeldingKafkaMessage)

        logger.info("Sent sykmelding with id=${sykmelding.sykmeldingId} to Kafka")
    }
}
