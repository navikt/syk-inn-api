package modules.sykmeldinger.db.exposed

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.syfo.rules.juridiskvurdering.JuridiskVurderingResult
import no.nav.syfo.rules.juridiskvurdering.JuridiskVurderingStatus
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

private val juridiskVurderingObjectMapper =
    jacksonObjectMapper().apply { registerModule(JavaTimeModule()) }

object JuridiskVurderingTable : Table("rule_status") {
    val sykmeldingId = javaUUID("sykmelding_id")
    val status = enumeration<JuridiskVurderingStatus>("status")
    val eventTimestamp = timestampWithTimeZone("event_timestamp")
    val juridiskVurdering =
        jsonb<JuridiskVurderingResult>(
            "juridisk_vurdering",
            serialize = { juridiskVurderingObjectMapper.writeValueAsString(it) },
            deserialize = { juridiskVurderingObjectMapper.readValue(it) },
        )
}
