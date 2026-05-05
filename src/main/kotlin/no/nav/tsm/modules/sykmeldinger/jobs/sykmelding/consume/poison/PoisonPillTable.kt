package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison

import java.time.OffsetDateTime
import java.util.UUID
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.core.java.javaUUID
import org.jetbrains.exposed.v1.javatime.timestampWithTimeZone

data class PoisonedSykmelding(val id: UUID, val reason: String, val created: OffsetDateTime)

object SykmeldingPoisonPillTable : Table("sykmelding_poison_pill") {
    val sykmeldingId = javaUUID("sykmelding_id")
    val reason = text("reason")
    val created = timestampWithTimeZone("created")
}
