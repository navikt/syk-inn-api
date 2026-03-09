package modules.kafka.consume

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import core.logger
import io.opentelemetry.instrumentation.annotations.WithSpan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import modules.sykmeldinger.SykmeldingerService
import modules.sykmeldinger.domain.VerifiedSykInnSykmelding

class SykmeldingConsumerService(
    private val consumer: SykmeldingConsumer,
    private val sykmeldingerService: SykmeldingerService,
) {
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
    private suspend fun handleSykmelding(key: String, value: Map<String, String>?) {
        logger.info(
            "Handling record with key $key and value of size ${value?.size ?: "null"}, values: ${
                jacksonObjectMapper().writeValueAsString(
                    value
                )
            }"
        )

        /**
         * TODO: The rules are already executed here, should we handle this is service or should
         *   Kafka rawdog Repo directly?
         */
        sykmeldingerService.insert(value.toSykInnSykmelding())
    }
}

/** Just a stub, should map from SykmledingKafkaRecord → SykInnSykmelding */
private fun Map<String, String>?.toSykInnSykmelding(): VerifiedSykInnSykmelding {
    TODO("Not yet implemented")
}
