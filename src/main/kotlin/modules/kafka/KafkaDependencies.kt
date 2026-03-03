package no.nav.tsm.modules.kafka

import io.ktor.server.application.Application
import io.ktor.server.config.ApplicationConfig
import io.ktor.server.plugins.di.dependencies
import java.util.Properties
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.serialization.ByteArrayDeserializer
import org.apache.kafka.common.serialization.StringDeserializer

fun Application.configureKafkaDependencies() {
    val environment = environment.config

    dependencies {
        provide<KafkaConsumer<String, ByteArray?>>("sykmeldinger") {
            initializeSykmeldingerConsumer(environment)
        }
        provide<KafkaSykmeldingerConsumer>(KafkaSykmeldingerConsumer::class)
        provide<KafkaConsumerJobManager>(KafkaConsumerJobManager::class)
    }
}

private fun initializeSykmeldingerConsumer(
    config: ApplicationConfig
): KafkaConsumer<String, ByteArray?> {
    val kafkaProperties =
        Properties().apply {
            config.config("kafka.config").toMap().forEach { this[it.key] = it.value }
        }

    println(kafkaProperties.toString())

    kafkaProperties.apply {
        this[ConsumerConfig.GROUP_ID_CONFIG] = "syk-inn-api-ktor"
        this[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
        this[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "true"
    }

    return KafkaConsumer(kafkaProperties, StringDeserializer(), ByteArrayDeserializer())
}
