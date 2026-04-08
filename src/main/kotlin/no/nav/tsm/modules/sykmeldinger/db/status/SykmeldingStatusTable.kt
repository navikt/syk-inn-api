package no.nav.tsm.modules.sykmeldinger.db.status

import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.datetime.timestampWithTimeZone

enum class SykmeldingStatusStatus {
    PENDING,
    SENDING,
    SENT,
    FAILED,
}

object SykmeldingStatusTable : Table("sykmelding_status") {
    val sykmeldingId = javaUUID("sykmelding_id")
    val status = text("status")
    val mottattTimestamp = timestampWithTimeZone("mottatt_timestamp")
    val eventTimestamp = timestampWithTimeZone("event_timestamp")
    val sendTimestamp = timestampWithTimeZone("send_timestamp")
    val sourceSystem = text("source")
}
