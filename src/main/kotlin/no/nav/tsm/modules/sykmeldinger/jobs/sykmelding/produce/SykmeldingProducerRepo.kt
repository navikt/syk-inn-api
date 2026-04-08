package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

data class SykmeldingStatusJob(val sykmeldingId: UUID, val status: SykmeldingStatusStatus)

class SykmeldingProducerRepo {

    fun getNext(): SykmeldingStatusJob? {
        return transaction {
            exec(
                """
                UPDATE sykmelding_status rs
                SET status = ?,
                    event_timestamp = now()
                FROM (
                    SELECT sykmelding_id FROM sykmelding_status
                    WHERE status = ?
                    ORDER BY send_timestamp
                    FOR UPDATE SKIP LOCKED
                    LIMIT 1
                ) AS temp_status
                WHERE rs.sykmelding_id = temp_status.sykmelding_id
                RETURNING rs.sykmelding_id, rs.status
                """
                    .trimIndent(),
                args =
                    listOf(
                        TextColumnType() to SykmeldingStatusStatus.SENDING.name,
                        TextColumnType() to SykmeldingStatusStatus.PENDING.name,
                    ),
                explicitStatementType = StatementType.EXEC,
            ) { rs ->
                if (rs.next()) {
                    SykmeldingStatusJob(
                        sykmeldingId = UUID.fromString(rs.getString("sykmelding_id")),
                        status = SykmeldingStatusStatus.valueOf(rs.getString("status")),
                    )
                } else {
                    null
                }
            }
        }
    }

    fun resetHangingJobs(timestamp: OffsetDateTime): Int {
        return transaction {
            SykmeldingStatusTable.update({
                (SykmeldingStatusTable.status inList
                    listOf(
                        SykmeldingStatusStatus.SENDING.name,
                        SykmeldingStatusStatus.FAILED.name,
                    )) and (SykmeldingStatusTable.eventTimestamp less timestamp)
            }) {
                it[status] = SykmeldingStatusStatus.PENDING.name
                it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }
    }
}
