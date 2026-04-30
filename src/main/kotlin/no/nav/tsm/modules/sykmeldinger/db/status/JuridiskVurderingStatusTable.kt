package no.nav.tsm.modules.sykmeldinger.db.status

import no.nav.tsm.core.db.jacksonJsonb
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

object JuridiskVurderingStatusTable : Table("juridisk_status") {
    val sykmeldingId = javaUUID("sykmelding_id")
    val status = text("status")
    val eventTimestamp = timestampWithTimeZone("event_timestamp")
    val juridiskVurdering = jacksonJsonb<List<RegulaJuridiskVurdering>>("juridisk_vurdering")
}
