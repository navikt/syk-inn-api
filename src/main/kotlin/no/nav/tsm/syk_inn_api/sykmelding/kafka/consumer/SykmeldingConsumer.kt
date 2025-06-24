package no.nav.tsm.syk_inn_api.sykmelding.kafka.consumer

import java.time.LocalDate
import java.time.Month
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingType
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMapper
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.secureLogger
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SykmeldingConsumer(
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    @param:Value($$"${nais.cluster}") private val clusterName: String
) {
    private val logger = logger()
    private val secureLog = secureLogger()

    @KafkaListener(
        topics = ["\${kafka.topics.sykmeldinger}"],
        groupId = "syk-inn-api-consumer",
        containerFactory = "kafkaListenerContainerFactory",
        batch = "false",
    )
    fun consume(record: ConsumerRecord<String, SykmeldingRecord?>) {
        val sykmeldingId = record.key()
        val value: SykmeldingRecord? = record.value()

        secureLog.info("Consuming record (id: $sykmeldingId): $value from topic ${record.topic()}")

        if (value == null) {
            handleTombstone(sykmeldingId)
            return
        }

        try {
            if (value.sykmelding.aktivitet.isEmpty()) {
                logger.warn(
                    "SykmeldingRecord with id=${record.key()} has no activity, skipping processing",
                )
                return
            }

            if (value.isBeforeYear(2024)) {
                return // Skip processing for sykmeldinger before 2024
            }

            if (value.sykmelding.type == SykmeldingType.UTENLANDSK) {
                return // skip processing for utenlandske sykmeldinger
            }

            val person: Person =
                personService.getPersonByIdent(value.sykmelding.pasient.fnr).getOrElse {
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
                sykmelderService
                    .sykmelder(
                        PersistedSykmeldingMapper.mapHprNummer(value),
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
                    sykmeldingRecord = value,
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
                "Kafka consumer failed, key: ${record.key()} - Error processing record, data: $value",
                e,
            )

            // Don't eat the exception, we don't want to commit on unexpected errors
            throw e
        }
    }

    private fun handleTombstone(sykmeldingId: String) {
        logger.info(
            "SykmeldingRecord is null (tombstone), deleting sykmelding with id=${sykmeldingId}",
        )
        sykmeldingPersistenceService.deleteSykmelding(sykmeldingId)
    }

    private fun SykmeldingRecord.isBeforeYear(year: Int): Boolean {
        val tom = this.sykmelding.aktivitet.first().tom
        return tom.isBefore(
            LocalDate.of(
                year,
                Month.JANUARY,
                1,
            ),
        )
    }
}
