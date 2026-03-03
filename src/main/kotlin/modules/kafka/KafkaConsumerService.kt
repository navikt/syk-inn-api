package no.nav.tsm.modules.kafka

import io.ktor.server.plugins.di.annotations.Named
import no.nav.tsm.core.logger
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.clients.producer.KafkaProducer

class KafkaConsumerService(
    @Named("sykmeldinger") val consumer: KafkaConsumer<String, ByteArray?>,
) {
    val logger = logger()

    init {
        println(consumer)
    }

    suspend fun start() {
        logger.info("Starting kafka consumer")
    }

    suspend fun stop() {}
}