package no.nav.tsm.modules.kafka

import io.ktor.server.plugins.di.annotations.Named
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import no.nav.tsm.core.logger
import org.apache.kafka.clients.consumer.KafkaConsumer

class KafkaSykmeldingerConsumer(
    @Named("sykmeldinger") val consumer: KafkaConsumer<String, ByteArray?>
) {
    private val topicName = "tsm.sykmeldinger"
    private val logger = logger()

    suspend fun consume() =
        withContext(Dispatchers.IO) {
            consumer.subscribe(listOf(topicName))

            try {
                while (isActive) {
                    val records = consumer.poll(Duration.ofMillis(10_000))
                    logger.info("Polled ${records.count()} records from kafka")

                    for (record in records) {
                        try {
                            handleRecord(record.key(), record.value())
                        } catch (cause: Exception) {
                            logger.error(
                                "Failed to handle record with key ${record.key()} and offset ${record.offset()}",
                                cause,
                            )
                        }
                    }
                }
            } finally {
                consumer.unsubscribe()
            }
        }

    // TODO: husk å lage nye OTEL traces per loop
    private fun handleRecord(key: String, value: ByteArray?) {
        logger.info("Handling record with key $key and value of size ${value?.size ?: "null"}")
    }
}
