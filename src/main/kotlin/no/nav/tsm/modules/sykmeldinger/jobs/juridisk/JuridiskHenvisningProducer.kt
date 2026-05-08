package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.Properties
import java.util.UUID
import no.nav.tsm.core.Environment
import no.nav.tsm.core.logger
import no.nav.tsm.regulus.regula.juridisk.JuridiskVurdering
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.Serializer
import org.apache.kafka.common.serialization.StringSerializer

class JuridiskHenvisningProducer(environment: Environment) {
    private val logger = logger()
    private val topicName = "teamsykmelding.paragraf-i-kode"

    private val kafkaProducer: KafkaProducer<String, JuridiskHenvisningRecord>

    init {
        val kafkaProperties = Properties(environment.kafka.config)

        kafkaProperties.apply {
            this[ProducerConfig.CLIENT_ID_CONFIG] = "syk-inn-api-juridisk-producer"
            this[ProducerConfig.ACKS_CONFIG] = "all"
            this[ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG] = "true"
            this[ProducerConfig.COMPRESSION_TYPE_CONFIG] = "gzip"
        }

        kafkaProducer =
            KafkaProducer(kafkaProperties, StringSerializer(), JuridiskVurderingRecordSerializer())
    }

    fun sendJuridiskVurderinger(sykmeldingId: UUID, juridiskVurderinger: List<JuridiskVurdering>) {
        val payload = JuridiskHenvisningRecord(juridiskVurderinger)
        val record = ProducerRecord(topicName, sykmeldingId.toString(), payload)

        val result = kafkaProducer.send(record).get()
        logger.debug(
            "Sent record with ${juridiskVurderinger.size} juridisk vurderinger to topic '$topicName' on partition ${result.partition()} offset ${result.offset()}"
        )
    }
}

/** The actual record to be published on Kafka. Don't rename any properties here. :-) */
data class JuridiskHenvisningRecord(val juridiskeVurderinger: List<JuridiskVurdering>)

private val juridiskVurderingKafkaObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

internal class JuridiskVurderingRecordSerializer : Serializer<JuridiskHenvisningRecord> {
    override fun serialize(topic: String, sykmeldingRecord: JuridiskHenvisningRecord): ByteArray? =
        juridiskVurderingKafkaObjectMapper.writeValueAsBytes(sykmeldingRecord)
}
