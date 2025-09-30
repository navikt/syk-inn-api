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
    @Value($$"${kafka.topics.sykmeldinger-input}") private lateinit var sykmeldingInputTopic: String

    @Bean
    @Profile("default")
    fun kafkaProducer(): SykmeldingInputProducer {
        return SykmeldingInputKafkaInputFactory.naisProducer()
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

            private val sourceNamespace = "tsm"
            private val sourceApp = "syk-inn-api"
            private val SOURCE_NAMESPACE_HEADER = "source-namespace"
            private val SOURCE_APP_HEADER = "source-app"

            override fun sendSykmelding(sykmeldingRecord: SykmeldingRecord) {
                val record =
                    ProducerRecord(
                        sykmeldingInputTopic,
                        sykmeldingRecord.sykmelding.id,
                        sykmeldingObjectMapper.writeValueAsBytes(sykmeldingRecord),
                    )
                record.headers().add(SOURCE_NAMESPACE_HEADER, sourceNamespace.toByteArray())
                record.headers().add(SOURCE_APP_HEADER, sourceApp.toByteArray())
                producer
                    .send(
                        record,
                    )
                    .get()
            }

            override fun sendSykmelding(
                sykmeldingRecord: SykmeldingRecord,
                sourceApp: String,
                sourceNamespace: String,
                additionalHeaders: Map<String, String>
            ) {
                TODO("Not yet implemented")
            }

            override fun tombstoneSykmelding(sykmeldingId: String) {
                val record =
                    ProducerRecord<String, ByteArray>(sykmeldingInputTopic, sykmeldingId, null)
                record.headers().add(SOURCE_NAMESPACE_HEADER, sourceNamespace.toByteArray())
                record.headers().add(SOURCE_APP_HEADER, sourceApp.toByteArray())
                producer.send(record).get()
            }

            override fun tombstoneSykmelding(
                sykmeldingId: String,
                sourceApp: String,
                sourceNamespace: String,
                additionalHeaders: Map<String, String>
            ) {
                TODO("Not yet implemented")
            }
        }

        return Producer()
    }
}
