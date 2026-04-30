package no.nav.tsm.modules.sykmeldinger.db.status

import no.nav.tsm.core.db.jacksonJsonb
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

enum class SykmeldingStatusStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
}

object SykmeldingStatusTable : Table("sykmelding_status") {
    val sykmeldingId = javaUUID("sykmelding_id")
    val status = text("status")
    val reason = jacksonJsonb<ReasonJsonb>("rule_reason").nullable()
    val mottattTimestamp = timestampWithTimeZone("mottatt_timestamp")
    val eventTimestamp = timestampWithTimeZone("event_timestamp")
    val sendTimestamp = timestampWithTimeZone("send_timestamp")
    val sourceSystem = text("source")
}

data class ReasonJsonb(val sykmeldt: String, val sykmelder: String)
