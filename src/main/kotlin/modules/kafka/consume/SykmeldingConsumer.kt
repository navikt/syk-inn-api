package no.nav.tsm.modules.kafka.consume

import no.nav.tsm.core.Environment
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer
import java.util.Properties

fun initializeSykmeldingerConsumer(
    environment: Environment
): KafkaConsumer<String, ByteArray?> {
    val kafkaProperties = Properties(environment.kafka)

    kafkaProperties.apply {
        this[ConsumerConfig.GROUP_ID_CONFIG] = "syk-inn-api-ktor"
        this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
    }

    return KafkaConsumer(kafkaProperties, StringDeserializer(), ByteArrayDeserializer())
}
