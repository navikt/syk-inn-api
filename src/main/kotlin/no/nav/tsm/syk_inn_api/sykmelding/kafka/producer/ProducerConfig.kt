package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import java.util.Properties
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.kafka.KafkaProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.kafka.annotation.EnableKafka

@Configuration
@EnableKafka
@EnableConfigurationProperties
class ProducerConfig {
    @Value($$"${kafka.topics.sykmeldinger-input}") private lateinit var sykmeldingInputTopic: String

    @Bean
    @Profile("default")
    fun kafkaProducer(): SykmeldingInputProducer {
        return SykmeldingInputKafkaInputFactory.naisProducer()
    }

    @Bean
    @Profile("local", "dev-kafka", "test")
    fun kafkaProducerLocal(props: KafkaProperties): SykmeldingInputProducer {
        return SykmeldingInputKafkaInputFactory.localProducer(
            "syk-inn-api",
            "tsm",
            Properties().apply { putAll(props.buildProducerProperties(null)) }
        )
    }
}
