package no.nav.tsm.syk_inn_api.sykmelding.kafka.consumer

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.syk_inn_api.person.PdlException
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprException
import no.nav.tsm.syk_inn_api.sykmelding.errors.ErrorRepository
import no.nav.tsm.syk_inn_api.sykmelding.errors.KafkaProcessingError
import no.nav.tsm.syk_inn_api.sykmelding.metrics.SykmeldingMetrics
import no.nav.tsm.syk_inn_api.sykmelding.metrics.SykmeldingServiceLevelIndicators
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMapper
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.scheduled.DAYS_OLD_SYKMELDING
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
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.LocalDate

@Component
class SykmeldingConsumer(
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    private val errorRepository: ErrorRepository,
    private val poisonPills: PoisonPills,
    private val sykmeldingMetrics: SykmeldingMetrics,
    private val sli: SykmeldingServiceLevelIndicators,
    @param:Value($$"${nais.cluster}") private val clusterName: String,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()

    @Transactional
    @KafkaListener(
        topics = [$$"${kafka.topics.sykmeldinger}"],
        groupId = "syk-inn-api-consumer",
        containerFactory = "kafkaListenerContainerFactory",
        batch = "false",
    )
    fun consume(record: ConsumerRecord<String, ByteArray?>) {
        val startTime = Instant.now()
        sykmeldingMetrics.incrementKafkaMessageConsumed()

        val sykmeldingId = record.key()
        if (poisonPills.isPoisoned(sykmeldingId)) {
            logger.warn("Sykmelding with id=$sykmeldingId is poisoned, skipping processing")
            sykmeldingMetrics.incrementKafkaPoisonPill()
            return
        }

        val value: ByteArray? = record.value()

        if (value == null) {
            logger.info(
                "SykmeldingRecord is null (tombstone), deleting sykmelding with id=${sykmeldingId}",
            )
            handleTombstone(sykmeldingId)
            sykmeldingMetrics.incrementKafkaTombstoneProcessed()
            return
        }

        try {

            val sykmeldingRecord = sykmeldingObjectMapper.readValue<SykmeldingRecord>(value)

            if (
                sykmeldingRecord.sykmelding.aktivitet.maxOf { it.tom } <
                LocalDate.now().minusDays(DAYS_OLD_SYKMELDING)
            ) {
                return // Skip processing for sykmeldinger before 2024
            }

            if (sykmeldingRecord.sykmelding.aktivitet.isEmpty()) {
                logger.warn(
                    "SykmeldingRecord with id=${record.key()} has no activity, skipping processing",
                )
                return
            }

            if (
                sykmeldingRecord.sykmelding.type ==
                no.nav.tsm.sykmelding.input.core.model.SykmeldingType.UTENLANDSK
            ) {
                return // skip processing for utenlandske sykmeldinger
            }

            val person: Person =
                personService.getPersonByIdent(sykmeldingRecord.sykmelding.pasient.fnr).getOrThrow()

            val sykmelder = findHprNumber(sykmeldingRecord).getOrThrow()
            sykmeldingPersistenceService.updateSykmelding(
                sykmeldingId = sykmeldingId,
                sykmeldingRecord = sykmeldingRecord,
                person = person,
                sykmelder = sykmelder,
            )

            // Record successful processing
            sykmeldingMetrics.recordKafkaProcessingDuration(
                Duration.between(
                    startTime,
                    Instant.now(),
                ),
            )
            sli.updateConsumerProcessingTimestamp()
        } catch (e: JacksonException) {
            sykmeldingMetrics.incrementKafkaProcessingError("jackson_error")
            handleError(record, e)
        } catch (pdlException: PdlException) {
            sykmeldingMetrics.incrementKafkaProcessingError("pdl_error")
            handleError(record, pdlException)
        } catch (hprException: HprException) {
            sykmeldingMetrics.incrementKafkaProcessingError("hpr_error")
            handleError(record, hprException)
        } catch (e: Exception) {
            sykmeldingMetrics.incrementKafkaProcessingError("unknown_error")
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

    private fun handleError(record: ConsumerRecord<String, ByteArray?>, e: Exception) {
        logger.error("Error processing record, key: ${record.key()}, error: ${e.message}", e)
        val kafkaProcessingError =
            KafkaProcessingError(
                kafkaOffset = record.offset(),
                kafkaPartition = record.partition(),
                key = record.key(),
                error = e.message,
                stackTrace = e.stackTraceToString(),
                partitionOffset = "${record.partition()}:${record.offset()}",
            )
        errorRepository.save(kafkaProcessingError)
    }

    private fun handleTombstone(sykmeldingId: String) {
        sykmeldingPersistenceService.deleteSykmelding(sykmeldingId)
    }

    private fun findHprNumber(sykmeldingRecord: SykmeldingRecord): Result<Sykmelder.Enkel> {
        val hprNummer = PersistedSykmeldingMapper.mapHprNummer(sykmeldingRecord)
        if (hprNummer != null) {
            val byHpr = sykmelderService.sykmelder(hprNummer, sykmeldingRecord.sykmelding.id)
            if (byHpr.isSuccess) {
                return byHpr
            }
            logger.warn(
                "Sykmelder not found in Helsenett Proxy by, hprNummer: $hprNummer, trying with fnr",
            )
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

        if (sykmelderFnr == null) {
            return Result.failure(
                HprException("Sykmelder not found in Helsenett Proxy by HPR and FNR", null),
            )
        }

        return sykmelderService.sykmelderByFnr(sykmelderFnr, requireNotNull(sykmeldingId))
    }
}
