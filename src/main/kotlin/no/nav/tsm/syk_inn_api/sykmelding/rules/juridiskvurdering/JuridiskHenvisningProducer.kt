package no.nav.tsm.syk_inn_api.sykmelding.rules.juridiskvurdering

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.syk_inn_api.sykmelding.rules.JuridiskVurderingResult
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class JuridiskHenvisningProducer(
    private val juridiskHenvisningKafkaProducer: KafkaProducer<String, ByteArray>,
    @Value($$"${kafka.topics.paragraf-i-kode}") private val topic: String,
) {
    val objectMapper: ObjectMapper =
        ObjectMapper().apply {
            registerKotlinModule()
            registerModule(JavaTimeModule())
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }

    fun send(sykmeldingId: String, juridiskVurderingResult: JuridiskVurderingResult) {
        val value = objectMapper.writeValueAsBytes(juridiskVurderingResult)
        juridiskHenvisningKafkaProducer.send(ProducerRecord(topic, sykmeldingId, value)).get()
    }
}
