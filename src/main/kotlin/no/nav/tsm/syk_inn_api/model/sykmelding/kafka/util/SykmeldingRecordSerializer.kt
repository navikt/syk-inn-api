package no.nav.tsm.mottak.sykmelding.kafka.util

import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.SykmeldingRecord
import no.nav.tsm.syk_inn_api.model.sykmelding.kafka.objectMapper
import org.apache.kafka.common.serialization.Serializer

class SykmeldingRecordSerializer : Serializer<SykmeldingRecord> {
    override fun serialize(topic: String, data: SykmeldingRecord?): ByteArray? {
        if (data != null) {
            return objectMapper.writeValueAsBytes(data)
        }
        return null
    }
}
