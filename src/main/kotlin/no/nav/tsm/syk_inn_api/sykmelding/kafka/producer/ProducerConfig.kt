package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
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
    fun kafkaProducer(props: KafkaProperties): SykmeldingInputProducer {
        return SykmeldingInputKafkaInputFactory.create()
    }
}
