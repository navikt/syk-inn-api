package no.nav.tsm.mottak.sykmelding.kafka.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import org.apache.kafka.common.serialization.Deserializer

class SykmeldingDeserializer : Deserializer<SykmeldingRecord> {

    private val objectMapper: ObjectMapper =
        jacksonObjectMapper().apply {
            registerKotlinModule()
            //            registerModule(SykmeldingModule()) //TODO Slett?
            registerModule(JavaTimeModule())
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
        }

    override fun deserialize(topic: String, p1: ByteArray): SykmeldingRecord {
        return objectMapper.readValue(p1)
    }
}
