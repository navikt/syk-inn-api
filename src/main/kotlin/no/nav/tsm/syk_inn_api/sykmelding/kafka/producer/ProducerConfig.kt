package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import no.nav.tsm.syk_inn_api.sykmelding.kafka.util.SykmeldingRecordSerializer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
@EnableConfigurationProperties
class ProducerConfig {

    @Bean
    fun kafkaProducer(props: KafkaProperties): KafkaProducer<String, SykmeldingRecord> {
        val producer =
            KafkaProducer(
                props.buildProducerProperties(null).apply {
                    put(ProducerConfig.ACKS_CONFIG, "all")
                    put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "gzip")
                    put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE)
                },
                StringSerializer(),
                SykmeldingRecordSerializer(),
            )
        return producer
    }
}
