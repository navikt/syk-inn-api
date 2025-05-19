package no.nav.tsm.mottak.sykmelding.kafka.util

import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import org.apache.kafka.common.serialization.Deserializer

class SykmeldingDeserializer : Deserializer<SykmeldingRecord> {

    override fun deserialize(topic: String, p1: ByteArray): SykmeldingRecord {
        return objectMapper.readValue(p1)
    }
}
