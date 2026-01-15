package no.nav.tsm.syk_inn_api.sykmelding.kafka.consumer

import com.fasterxml.jackson.core.JacksonException
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.syk_inn_api.person.PdlException
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelder.SykmelderService
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprException
import no.nav.tsm.syk_inn_api.sykmelding.errors.ErrorRepository
import no.nav.tsm.syk_inn_api.sykmelding.errors.KafkaProcessingError
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMapper
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistence
import no.nav.tsm.syk_inn_api.sykmelding.scheduled.DAYS_OLD_SYKMELDING
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.Papirsykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.XmlSykmelding
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.util.*

@Component
class SykmeldingConsumer(
    private val personService: PersonService,
    private val sykmelderService: SykmelderService,
    private val errorRepository: ErrorRepository,
    private val sykmeldingPersistence: SykmeldingPersistence,
) {
    private val logger = logger()
    private val teamLogger = teamLogger()
    val objectMapper =
        jacksonObjectMapper().apply {
            registerModule(SykmeldingModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
    @Transactional
    @KafkaListener(
        topics = [$$"${kafka.topics.sykmeldinger}"],
        groupId = "syk-inn-api-consumer",
        containerFactory = "kafkaListenerContainerFactory",
        batch = "false",
    )
    fun consume(record: ConsumerRecord<String, ByteArray?>) {
        val sykmeldingId = record.key()
        val value: ByteArray? = record.value()

        if (value == null) {
            logger.info(
                "SykmeldingRecord is null (tombstone), deleting sykmelding with id=${sykmeldingId}",
            )
            handleTombstone(sykmeldingId)
            return
        }

        try {

            val sykmeldingRecord = objectMapper.readValue<SykmeldingRecord>(value)

            if (
                sykmeldingRecord.sykmelding.aktivitet.maxOf { it.tom } <
                    LocalDate.now().minusDays(DAYS_OLD_SYKMELDING)
            ) {
                return
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
            val sykmeldingDb =
                mapSykmeldingRecordToSykmeldingDatabaseEntity(
                    sykmeldingId,
                    sykmeldingRecord,
                    true,
                    person,
                    sykmelder
                )
            sykmeldingPersistence.saveSykmelding(sykmeldingDb)
        } catch (e: JacksonException) {
            handleError(record, e)
        } catch (pdlException: PdlException) {
            handleError(record, pdlException)
        } catch (hprException: HprException) {
            handleError(record, hprException)
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
        sykmeldingPersistence.deleteSykmelding(sykmeldingId)
    }

    private fun findHprNumber(sykmeldingRecord: SykmeldingRecord): Result<Sykmelder.Enkel> {
        val hprNummer = PersistedSykmeldingMapper.mapHprNummer(sykmeldingRecord)
        if (hprNummer != null) {
            val byHpr = sykmelderService.sykmelder(hprNummer, sykmeldingRecord.sykmelding.id)
            if (byHpr.isSuccess) {
                return byHpr
            }
            logger.warn(
                "Sykmelder not found in Helsenett Proxy by, hprNummer: $hprNummer, trying with fnr"
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
                HprException("Sykmelder not found in Helsenett Proxy by HPR and FNR", null)
            )
        }

        return sykmelderService.sykmelderByFnr(sykmelderFnr, requireNotNull(sykmeldingId))
    }

    fun mapSykmeldingRecordToSykmeldingDatabaseEntity(
        sykmeldingId: String,
        sykmeldingRecord: SykmeldingRecord,
        validertOk: Boolean,
        person: Person,
        sykmelder: Sykmelder,
    ): SykmeldingDb {
        val persistedSykmelding =
            PersistedSykmeldingMapper.mapSykmeldingRecordToPersistedSykmelding(
                sykmeldingRecord,
                person,
                sykmelder,
            )
        return SykmeldingDb(
            sykmeldingId = sykmeldingId,
            idempotencyKey = UUID.randomUUID(),
            mottatt = sykmeldingRecord.sykmelding.metadata.mottattDato,
            pasientIdent = sykmeldingRecord.sykmelding.pasient.fnr,
            sykmelderHpr = sykmelder.hpr,
            sykmelding = persistedSykmelding,
            legekontorOrgnr = PersistedSykmeldingMapper.mapLegekontorOrgnr(sykmeldingRecord),
            legekontorTlf = PersistedSykmeldingMapper.mapLegekontorTlf(sykmeldingRecord),
            fom = persistedSykmelding.aktivitet.minOf { it.fom },
            tom = persistedSykmelding.aktivitet.maxOf { it.tom },
            validationResult =
                PersistedSykmeldingMapper.mapValidationResult(sykmeldingRecord.validation)
        )
    }
}
