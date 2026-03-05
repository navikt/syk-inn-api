package no.nav.tsm.modules.kafka.consume

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import core.logger

class SykmeldingConsumerService(private val consumer: SykmeldingConsumer) {
    private val logger = logger()

    suspend fun consume() =
        withContext(Dispatchers.IO) {
            consumer.subscribe()

            try {
                while (isActive) {
                    val records = consumer.poll()

                    for ((key, sykmelding) in records) {
                        handleSykmelding(key, sykmelding)
                    }
                }
            } catch (ex: Exception) {
                logger.error("Kafka consumer loop threw an exception", ex)
                throw ex
            } finally {
                consumer.unsubscribe()
            }
        }

    @WithSpan
    private fun handleSykmelding(key: String, value: Map<String, String>?) {
        logger.info(
            "Handling record with key $key and value of size ${value?.size ?: "null"}, values: ${
                jacksonObjectMapper().writeValueAsString(
                    value
                )
            }"
        )
    }
}
