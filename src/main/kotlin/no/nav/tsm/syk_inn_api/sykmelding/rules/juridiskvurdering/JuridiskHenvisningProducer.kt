package no.nav.tsm.syk_inn_api.sykmelding.rules.juridiskvurdering

import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.ProducerRecord
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Component
class JuridiskHenvisningProducer(
    private val juridiskHenvisningKafkaProducer: KafkaProducer<String, ByteArray>,
    @Value($$"${kafka.topics.paragraf-i-kode}") private val topic: String,
) {
    val objectMapper: ObjectMapper = jsonMapper {
        addModule(kotlinModule())
        disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun send(sykmeldingId: String, juridiskVurderingResult: JuridiskVurderingResult) {
        val value = objectMapper.writeValueAsBytes(juridiskVurderingResult)
        juridiskHenvisningKafkaProducer.send(ProducerRecord(topic, sykmeldingId, value)).get()
    }
}
