package no.nav.tsm.syk_inn_api.sykmelding.kafka.consumer

import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.util.SykmeldingDeserializer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.DefaultKafkaConsumerFactory

@Configuration
@EnableKafka
@EnableConfigurationProperties
class ConsumerConfig {
    @Bean
    fun kafkaListenerContainerFactory(
        props: KafkaProperties,
        errorHandler: ConsumerErrorHandler
    ): ConcurrentKafkaListenerContainerFactory<String, SykmeldingRecord> {
        val consumerFactory =
            DefaultKafkaConsumerFactory(
                props.buildConsumerProperties(null).apply {
                    put(
                        org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,
                        "earliest",
                    )
                    put(org.apache.kafka.clients.consumer.ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 1)
                    put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true)
                },
                StringDeserializer(),
                SykmeldingDeserializer(),
            )

        val factory = ConcurrentKafkaListenerContainerFactory<String, SykmeldingRecord>()
        factory.consumerFactory = consumerFactory
        factory.setCommonErrorHandler(errorHandler)
        return factory
    }
}
