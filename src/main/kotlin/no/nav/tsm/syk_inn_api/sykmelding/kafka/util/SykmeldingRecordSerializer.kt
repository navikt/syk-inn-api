package no.nav.tsm.syk_inn_api.sykmelding.kafka.util

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.syk_inn_api.sykmelding.kafka.SykmeldingModule
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.SykmeldingRecord
import org.apache.kafka.common.serialization.Serializer

class SykmeldingRecordSerializer : Serializer<SykmeldingRecord> {
    override fun serialize(topic: String, data: SykmeldingRecord?): ByteArray? {
        if (data != null) {
            return objectMapper.writeValueAsBytes(data)
        }
        return null
    }
}

val objectMapper: ObjectMapper =
    ObjectMapper().apply {
        registerKotlinModule()
        registerModule(SykmeldingModule())
        registerModule(JavaTimeModule())
        configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
