package no.nav.tsm.mottak.sykmelding.kafka.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import org.apache.kafka.common.serialization.Deserializer
import kotlin.reflect.KClass

class SykmeldingDeserializer<T : Any>(private val type: KClass<T>) : Deserializer<T> {

    private val objectMapper: ObjectMapper =
        jacksonObjectMapper().apply {
            registerKotlinModule()
//            registerModule(SykmeldingModule()) //TODO Slett?
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }

    override fun deserialize(topic: String, p1: ByteArray): T {
        return objectMapper.readValue(p1, type.java)
    }
}
