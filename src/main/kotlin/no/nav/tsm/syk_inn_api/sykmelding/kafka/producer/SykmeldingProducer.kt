package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import org.springframework.stereotype.Component

@Component
class SykmeldingProducer(
    private val kafkaProducer: SykmeldingInputProducer,
) {

    private val logger = logger()

    fun send(
        sykmeldingId: String,
        sykmelding: SykmeldingDocument,
        person: Person,
        sykmelder: Sykmelder,
        validationResult: ValidationResult,
        source: String,
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
                        source,
                    ),
                validation = validationResult,
            )
        kafkaProducer.sendSykmelding(sykmeldingKafkaMessage)

        logger.info("Sent sykmelding with id=$sykmeldingId to Kafka")
    }
}
