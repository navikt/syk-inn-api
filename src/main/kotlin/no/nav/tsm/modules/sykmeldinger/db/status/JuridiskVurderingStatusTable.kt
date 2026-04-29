package no.nav.tsm.modules.sykmeldinger.db.status

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone
import org.jetbrains.exposed.v1.json.jsonb

private val juridiskVurderingExposedObjectMapper =
    jacksonObjectMapper().apply { registerModule(JavaTimeModule()) }

object JuridiskVurderingStatusTable : Table("juridisk_status") {
    val sykmeldingId = javaUUID("sykmelding_id")
    val status = text("status")
    val eventTimestamp = timestampWithTimeZone("event_timestamp")
    val juridiskVurdering =
        jsonb<List<RegulaJuridiskVurdering>>(
            "juridisk_vurdering",
            serialize = { juridiskVurderingExposedObjectMapper.writeValueAsString(it) },
            deserialize = { juridiskVurderingExposedObjectMapper.readValue(it) },
        )
}
