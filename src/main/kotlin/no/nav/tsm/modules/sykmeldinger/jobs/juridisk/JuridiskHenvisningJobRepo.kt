package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import com.fasterxml.jackson.module.kotlin.readValue
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlinx.coroutines.flow.firstOrNull
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.core.db.exposedJacksonObjectMapper
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingStatusTable
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.r2dbc.update

data class JuridiskHenvisningJob(
    val sykmeldingId: UUID,
    val status: JuridiskVurderingStatus,
    val juridiskVurdering: List<RegulaJuridiskVurdering>,
)

class JuridiskHenvisningJobRepo {

    suspend fun getNext(): JuridiskHenvisningJob? {
        return dbQuery {
            exec(
                    """
                    UPDATE juridisk_status rs
                    SET status = ?,
                        event_timestamp = now()
                    FROM (
                        SELECT sykmelding_id FROM juridisk_status
                        WHERE status = ?
                        ORDER BY event_timestamp
                        FOR UPDATE SKIP LOCKED
                        LIMIT 1
                    ) AS temp_status
                    WHERE rs.sykmelding_id = temp_status.sykmelding_id
                    RETURNING rs.sykmelding_id, rs.status, rs.juridisk_vurdering
                    """
                        .trimIndent(),
                    args =
                        listOf(
                            TextColumnType() to JuridiskVurderingStatus.SENDING.name,
                            TextColumnType() to JuridiskVurderingStatus.PENDING.name,
                        ),
                    explicitStatementType = StatementType.UPDATE,
                ) {
                    JuridiskHenvisningJob(
                        sykmeldingId = UUID.fromString(it.get("sykmelding_id", String::class.java)),
                        status =
                            JuridiskVurderingStatus.valueOf(it.get("status", String::class.java)),
                        juridiskVurdering =
                            exposedJacksonObjectMapper.readValue(
                                it.get("juridisk_vurdering", String::class.java)
                            ),
                    )
                }
                ?.firstOrNull()
        }
    }

    suspend fun updateStatus(sykmeldingId: UUID, newStatus: JuridiskVurderingStatus) = dbQuery {
        JuridiskVurderingStatusTable.update({
            JuridiskVurderingStatusTable.sykmeldingId eq sykmeldingId
        }) {
            it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
            it[status] = newStatus.name
        }
    }

    suspend fun resetHangingJobs(timestamp: OffsetDateTime): Int {
        return dbQuery {
            JuridiskVurderingStatusTable.update({
                (JuridiskVurderingStatusTable.status inList
                    listOf(
                        JuridiskVurderingStatus.SENDING.name,
                        JuridiskVurderingStatus.FAILED.name,
                    )) and (JuridiskVurderingStatusTable.eventTimestamp less timestamp)
            }) {
                it[status] = JuridiskVurderingStatus.PENDING.name
                it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }
    }
}
