package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlinx.coroutines.flow.firstOrNull
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.core.db.exposedJacksonObjectMapper
import no.nav.tsm.modules.sykmeldinger.db.status.ReasonJsonb
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.r2dbc.update

data class SykmeldingStatusJob(
    val sykmeldingId: UUID,
    val status: SykmeldingStatusStatus,
    val reason: ReasonJsonb?,
)

class SykmeldingProducerRepo {
    suspend fun getNext(): SykmeldingStatusJob? {
        return dbQuery {
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
                    RETURNING rs.sykmelding_id, rs.status, rs.rule_reason
                    """
                        .trimIndent(),
                    args =
                        listOf(
                            TextColumnType() to SykmeldingStatusStatus.SENDING.name,
                            TextColumnType() to SykmeldingStatusStatus.PENDING.name,
                        ),
                    explicitStatementType = StatementType.EXEC,
                ) {
                    SykmeldingStatusJob(
                        sykmeldingId = UUID.fromString(it.get("sykmelding_id", String::class.java)),
                        status =
                            SykmeldingStatusStatus.valueOf(it.get("status", String::class.java)),
                        reason =
                            it.get("rule_reason", String::class.java)?.let {
                                exposedJacksonObjectMapper.readValue<ReasonJsonb>(it)
                            },
                    )
                }
                ?.firstOrNull()
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
