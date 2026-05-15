package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Duration
import java.util.*
import kotlin.time.toJavaDuration
import no.nav.tsm.core.Environment
import no.nav.tsm.core.logger
import no.nav.tsm.core.teamLogger
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison.SykmeldingPoisonPillRepo
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer

class SykmeldingConsumer(
    environment: Environment,
    private val sykmeldingPoisonPillRepo: SykmeldingPoisonPillRepo,
) {
    private val logger = logger()
    private val teamLog = teamLogger()

    private val topicName = "tsm.sykmeldinger"
    private val groupId = "syk-inn-api-v2"

    private val duration: Duration = environment.kafka.sykmeldingConsumer.longPoll.toJavaDuration()
    private val consumer: KafkaConsumer<String, ByteArray?>

    init {
        val kafkaProperties = Properties(environment.kafka.config)

        kafkaProperties.apply {
            this[ConsumerConfig.GROUP_ID_CONFIG] = groupId
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        }

        consumer = KafkaConsumer(kafkaProperties, StringDeserializer(), ByteArrayDeserializer())
    }

    suspend fun poll(): List<Pair<String, SykmeldingRecord?>> {
        val records = consumer.poll(duration)
        if (records.isEmpty) return emptyList()

        logger.debug("Sykmelding consumer polled ${records.count()} records from ${topicName}")
        return records.map { tryParse(it) }
    }

    fun subscribe() {
        logger.info("Subscribing $topicName")
        consumer.subscribe(listOf(topicName))
    }

    fun unsubscribe() {
        logger.info("Unsubscribing $topicName")
        consumer.unsubscribe()
    }

    private suspend fun tryParse(
        record: ConsumerRecord<String, ByteArray?>
    ): Pair<String, SykmeldingRecord?> =
        try {
            return record.key() to record.value()?.let { parseAndMapSykmelding(it) }
        } catch (ex: Exception) {
            record.value()?.let {
                teamLog.warn("Full failing sykmelding JSON: ${String(bytes = it)}")
            }

            try {
                val key = UUID.fromString(record.key())
                val poisoned = sykmeldingPoisonPillRepo.isPoisoned(key)
                if (poisoned != null) {
                    logger.warn(
                        "Found poisoned sykmelding ${record.key()}, reason ${poisoned.reason} at ${poisoned.created}"
                    )

                    return record.key() to null
                }
            } catch (poisonEx: Exception) {
                logger.error(
                    "Unable to check if sykmelding was poisoned, throwing original error",
                    poisonEx,
                )
                return record.key() to null
            }

            throw IllegalStateException(
                "Got exception during record deserialization for record with key ${record.key()} and offset ${record.offset()} size (${record.value()?.size ?: "empty"})",
                ex,
            )
        }

    private fun parseAndMapSykmelding(bytes: ByteArray): SykmeldingRecord {
        return recordObjectMapper.readValue<SykmeldingRecord>(bytes)
    }

    private val recordObjectMapper =
        jacksonObjectMapper().apply {
            registerModule(SykmeldingModule())
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }
}
