package no.nav.tsm.utils

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.kotest.matchers.equals.shouldEqual
import java.util.Properties
import kotlin.collections.set
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.sykmelding.input.core.model.SykmeldingModule
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import org.testcontainers.kafka.ConfluentKafkaContainer

object KafkaTestConsumer {
    private val mapper =
        jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(SykmeldingModule())
        }

    fun parseIt(record: ByteArray?): SykmeldingRecord? =
        if (record != null) mapper.readValue(record) else null

    fun createTestConsumer(container: ConfluentKafkaContainer): KafkaConsumer<String, ByteArray?> {
        val kafkaProperties =
            Properties().apply { this["bootstrap.servers"] = container.bootstrapServers }

        println(container.bootstrapServers)

        kafkaProperties.apply {
            this[ConsumerConfig.GROUP_ID_CONFIG] = "syk-inn-api-ktor"
            this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
        }

        val kafkaConsumer =
            KafkaConsumer(kafkaProperties, StringDeserializer(), ByteArrayDeserializer())
        kafkaConsumer.subscribe(listOf("tsm.sykmeldinger-input"))

        return kafkaConsumer
    }
}

object KafkaTestUtils {
    fun expectAllValues(sykmelding: BehandlerSykmeldingFull, record: SykmeldingRecord) {
        sykmelding.sykmeldingId.toString() shouldEqual record.sykmelding.id
        // TODO: Assert all the values
    }
}
