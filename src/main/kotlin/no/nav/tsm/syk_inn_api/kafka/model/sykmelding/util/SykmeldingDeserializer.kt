package no.nav.tsm.syk_inn_api.kafka.model.sykmelding.util

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.syk_inn_api.kafka.model.sykmelding.SykmeldingRecord
import org.apache.kafka.common.serialization.Deserializer

class SykmeldingDeserializer : Deserializer<SykmeldingRecord> {

    override fun deserialize(topic: String, p1: ByteArray): SykmeldingRecord {
        return objectMapper.readValue(p1)
    }
}
