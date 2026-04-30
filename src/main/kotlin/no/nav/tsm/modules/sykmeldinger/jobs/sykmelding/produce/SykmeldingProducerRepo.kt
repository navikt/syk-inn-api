package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlinx.coroutines.flow.firstOrNull
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.status.ReasonJsonb
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus.PENDING
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus.SENDING
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption.PostgreSQL.ForUpdate
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption.PostgreSQL.MODE.SKIP_LOCKED
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.updateReturning

data class SykmeldingStatusJob(
    val sykmeldingId: UUID,
    val status: SykmeldingStatusStatus,
    val reason: ReasonJsonb?,
)

class SykmeldingProducerRepo {
    suspend fun getNext(): SykmeldingStatusJob? = dbQuery {
        with(SykmeldingStatusTable) {
            updateReturning(
                    listOf(sykmeldingId, status, reason),
                    {
                        sykmeldingId inSubQuery
                            select(sykmeldingId)
                                .where { status eq PENDING.name }
                                .orderBy(sendTimestamp)
                                .limit(1)
                                .forUpdate(ForUpdate(SKIP_LOCKED))
                    },
                ) {
                    it[status] = SENDING.name
                    it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
                }
                .firstOrNull()
                ?.let { row ->
                    SykmeldingStatusJob(
                        sykmeldingId = row[sykmeldingId],
                        status = SykmeldingStatusStatus.valueOf(row[status]),
                        reason = row[reason],
                    )
                }
        }
    }

    suspend fun updateStatus(sykmeldingId: UUID, newStatus: SykmeldingStatusStatus) = dbQuery {
        SykmeldingStatusTable.update({ SykmeldingStatusTable.sykmeldingId eq sykmeldingId }) {
            it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
            it[status] = newStatus.name
        }
    }

    suspend fun resetHangingJobs(timestamp: OffsetDateTime): Int {
        return dbQuery {
            SykmeldingStatusTable.update({
                (SykmeldingStatusTable.status inList
                    listOf(SENDING.name, SykmeldingStatusStatus.FAILED.name)) and
                    (SykmeldingStatusTable.eventTimestamp less timestamp)
            }) {
                it[status] = PENDING.name
                it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }
    }
}
