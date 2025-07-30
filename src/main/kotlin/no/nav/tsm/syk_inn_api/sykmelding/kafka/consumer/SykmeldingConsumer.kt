package no.nav.tsm.syk_inn_api.sykmelding.kafka.consumer

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMapper
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMapper.isBeforeYear
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.utils.PoisonPills
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.Papirsykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.XmlSykmelding
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component

@Component
class SykmeldingConsumer(
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    private val poisonPills: PoisonPills,
    @param:Value($$"${nais.cluster}") private val clusterName: String,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    @KafkaListener(
        topics = ["\${kafka.topics.sykmeldinger}"],
        groupId = "syk-inn-api-consumer",
        containerFactory = "kafkaListenerContainerFactory",
        batch = "false",
    )
    fun consume(record: ConsumerRecord<String, ByteArray?>) {
        val sykmeldingId = record.key()
        if (poisonPills.isPoisoned(sykmeldingId)) {
            logger.warn("Sykmelding with id=$sykmeldingId is poisoned, skipping processing")
            return
        }

        val value: ByteArray? = record.value()

        teamLogger.info("Consuming record (id: $sykmeldingId): $value from topic ${record.topic()}")

        if (value == null) {
            handleTombstone(sykmeldingId)
            return
        }

        try {
            val sykmeldingRecord = sykmeldingObjectMapper.readValue<SykmeldingRecord>(value)
            if (sykmeldingRecord.sykmelding.aktivitet.isEmpty()) {
                logger.warn(
                    "SykmeldingRecord with id=${record.key()} has no activity, skipping processing",
                )
                return
            }

            if (sykmeldingRecord.isBeforeYear(2024)) {
                return // Skip processing for sykmeldinger before 2024
            }

            if (
                sykmeldingRecord.sykmelding.type ==
                    no.nav.tsm.sykmelding.input.core.model.SykmeldingType.UTENLANDSK
            ) {
                return // skip processing for utenlandske sykmeldinger
            }

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
                sykmelderService
                    .sykmelder(
                        findHprNumber(sykmeldingRecord),
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
            teamLogger.error(
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

    private fun findHprNumber(sykmeldingRecord: SykmeldingRecord): String {
        val hprNummer = PersistedSykmeldingMapper.mapHprNummer(sykmeldingRecord)
        if (hprNummer != null) {
            return hprNummer
        }

        val sykmelding = sykmeldingRecord.sykmelding

        val (sykmelderFnr, sykmeldingId) =
            when (sykmelding) {
                is DigitalSykmelding ->
                    sykmelding.sykmelder.ids.find { it.type == PersonIdType.FNR }?.id to
                        sykmelding.id
                is Papirsykmelding ->
                    sykmelding.sykmelder.ids.find { it.type == PersonIdType.FNR }?.id to
                        sykmelding.id
                is XmlSykmelding ->
                    sykmelding.sykmelder.ids.find { it.type == PersonIdType.FNR }?.id to
                        sykmelding.id
                else -> null to null
            }

        requireNotNull(sykmelderFnr) { "Sykmelder not found in Helsenett Proxy" }

        return sykmelderService
            .sykmelderByFnr(sykmelderFnr, requireNotNull(sykmeldingId))
            .getOrElse {
                logger.error(
                    "Failed to get sykmelder hpr from a fnr - Sykmelder not found in Helsenett Proxy Exception",
                    it,
                )
                throw it
            }
            .hpr
    }
}
