package no.nav.tsm.syk_inn_api.sykmelding.kafka.producer

import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.sykmeldingObjectMapper
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputKafkaInputFactory
import no.nav.tsm.sykmelding.input.producer.SykmeldingInputProducer
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.ByteArraySerializer
import org.apache.kafka.common.serialization.StringSerializer
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
    @Value("\${kafka.topics.sykmeldinger-input}") private lateinit var sykmeldingInputTopic: String

    @Bean
    @Profile("default")
    fun kafkaProducer(props: KafkaProperties): SykmeldingInputProducer {
        return SykmeldingInputKafkaInputFactory.create()
    }

    @Bean
    @Profile("local", "dev-kafka", "test")
    fun kafkaProducerLocal(props: KafkaProperties): SykmeldingInputProducer {
        val producer =
            KafkaProducer(
                props.buildProducerProperties(null).apply {},
                StringSerializer(),
                ByteArraySerializer(),
            )

        class Producer : SykmeldingInputProducer {
            override fun sendSykmelding(sykmelding: SykmeldingRecord) {
                producer.send(
                    ProducerRecord(
                        sykmeldingInputTopic,
                        sykmelding.sykmelding.id,
                        sykmeldingObjectMapper.writeValueAsBytes(sykmelding),
                    ),
                )
            }

            override fun tombstoneSykmelding(sykmeldingId: String) {
                producer.send(ProducerRecord(sykmeldingInputTopic, sykmeldingId, null))
            }
        }

        return Producer()
    }
}
