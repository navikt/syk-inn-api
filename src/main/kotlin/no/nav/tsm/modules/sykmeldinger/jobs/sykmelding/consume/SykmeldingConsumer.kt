package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.Duration
import java.util.*
import no.nav.tsm.core.Environment
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer

class SykmeldingConsumer(environment: Environment) {
    private val logger = logger()
    private val topicName = "tsm.sykmeldinger"

    private val duration: Duration =
        Duration.ofMillis(environment.kafka.sykmeldingConsumer.longPoll)
    private val consumer: KafkaConsumer<String, ByteArray?>
    private val kafkaObjectMapper = jacksonObjectMapper()

    init {
        val kafkaProperties = Properties(environment.kafka.config)

        kafkaProperties.apply {
            this[ConsumerConfig.GROUP_ID_CONFIG] = "syk-inn-api-ktor"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        }

        consumer = KafkaConsumer(kafkaProperties, StringDeserializer(), ByteArrayDeserializer())
    }

    fun poll(): List<Pair<String, VerifiedSykInnSykmelding?>> {
        val records = consumer.poll(duration)
        if (records.isEmpty) return emptyList()

        logger.info("Got ${records.count()} records from kafka")
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

    private fun tryParse(
        record: ConsumerRecord<String, ByteArray?>
    ): Pair<String, VerifiedSykInnSykmelding?> =
        try {
            return record.key() to record.value()?.let { parseAndMapSykmelding(it) }
        } catch (ex: Exception) {
            throw IllegalStateException(
                "Got exception during record deserialization for record with key ${record.key()} and offset ${record.offset()}",
                ex,
            )
        }

    private fun parseAndMapSykmelding(bytes: ByteArray): VerifiedSykInnSykmelding {
        return sykmeldingObjectMapper.readValue<SykmeldingRecord>(bytes).toVerifiedSykmelding()
    }
}
