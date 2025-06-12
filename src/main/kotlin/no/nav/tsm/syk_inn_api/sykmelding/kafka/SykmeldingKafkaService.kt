package no.nav.tsm.syk_inn_api.sykmelding.kafka

import java.time.LocalDate
import java.time.Month
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HelsenettProxyService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingType
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMapper
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.secureLogger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Service

@Service
class SykmeldingKafkaService(
    private val kafkaProducer: KafkaProducer<String, SykmeldingRecord>,
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
    private val personService: PersonService,
    private val helsenettProxyService: HelsenettProxyService,
) {
    @Value("\${nais.cluster}") private lateinit var clusterName: String

    @Value("\${kafka.topics.sykmeldinger-input}") private lateinit var sykmeldingInputTopic: String

    private val logger = logger()
    private val secureLog = secureLogger()

    fun send(
        sykmeldingId: String,
        sykmelding: SykmeldingDocument,
        person: Person,
        sykmelder: HprSykmelder,
        regulaResult: RegulaResult,
    ) {
        try {
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
        } catch (e: Exception) {
            logger.error("Failed to send sykmelding with id=$sykmeldingId to Kafka")
            e.printStackTrace()
        }
    }

    @KafkaListener(
        topics = ["\${kafka.topics.sykmeldinger}"],
        groupId = "syk-inn-api-consumer",
        containerFactory = "kafkaListenerContainerFactory",
        batch = "false",
    )
    fun consume(record: ConsumerRecord<String, SykmeldingRecord>) {
        try {
            secureLog.info("Consuming record: ${record.value()} from topic ${record.topic()}")

            val tom = record.value().sykmelding.aktivitet.first().tom
            if (isVeryOldSykmelding(tom)) {
                return // Skip processing for sykmeldinger before 2024
            }

            if (record.value().sykmelding.type == SykmeldingType.UTENLANDSK) {
                return // skip processing for utenlandske sykmeldinger
            }

            if (record.value() == null) {
                logger.info(
                    "SykmeldingRecord is null (tombstone), deleting sykmelding with id=${record.key()}",
                )
                sykmeldingPersistenceService.deleteSykmelding(record.key())
                return
            }

            val sykmeldingRecord = record.value()
            val sykmeldingId = record.key()

            val person: Person =
                personService.getPersonByIdent(sykmeldingRecord.sykmelding.pasient.fnr).getOrElse {
                    logger.error(
                        "Kafka consumer failed, key: ${record.key()} - Person not found in Pdl Exception",
                        it,
                    )
                    if (clusterName == "dev-gcp") {
                        logger.warn("Person not found in dev-gcp, skipping sykmelding")
                        return
                    } else throw it
                }

            val sykmelder =
                helsenettProxyService
                    .getSykmelderByHpr(
                        PersistedSykmeldingMapper.mapHprNummer(sykmeldingRecord),
                        sykmeldingId,
                    )
                    .getOrElse {
                        logger.error(
                            "Kafka consumer failed, key: ${record.key()} - Sykmelder not found in Helsenett Proxy Exception",
                            it,
                        )
                        if (clusterName == "dev-gcp") {
                            logger.warn("Sykmelder not found in dev-gcp, skipping sykmelding")
                            return
                        } else throw it
                    }

            try {
                sykmeldingPersistenceService.updateSykmelding(
                    sykmeldingId = sykmeldingId,
                    sykmeldingRecord = sykmeldingRecord,
                    person = person,
                    sykmelder = sykmelder,
                )
            } catch (e: Exception) {
                logger.error(
                    "Kafka consumer failed, key: ${record.key()} - Unable to save to database",
                    e,
                )

                throw e
            }
        } catch (e: Exception) {
            logger.error(
                "Kafka consumer failed, key: ${record.key()} - Error processing record",
                e,
            )
            secureLog.error(
                "Kafka consumer failed, key: ${record.key()} - Error processing record, data: ${record.value()}",
                e,
            )
        }
    }

    private fun isVeryOldSykmelding(tom: LocalDate): Boolean {
        return tom.isBefore(
            LocalDate.of(
                2024,
                Month.JANUARY,
                1,
            ),
        )
    }
}
