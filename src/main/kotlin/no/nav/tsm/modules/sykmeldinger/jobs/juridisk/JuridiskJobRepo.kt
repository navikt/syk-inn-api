package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.tsm.modules.sykmeldinger.db.exposed.JuridiskVurderingTable
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskVurderingResult
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskVurderingStatus
import org.jetbrains.exposed.v1.core.TextColumnType
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.statements.StatementType
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.jetbrains.exposed.v1.jdbc.update

private val objectMapper = jacksonObjectMapper().apply { registerModule(JavaTimeModule()) }

data class JuridiskJob(
    val sykmeldingId: UUID,
    val status: JuridiskVurderingStatus,
    val juridiskVurdering: JuridiskVurderingResult,
)

class JuridiskJobRepo {

    fun getNext(): JuridiskJob? {
        return transaction {
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
                explicitStatementType = StatementType.EXEC,
            ) { rs ->
                if (rs.next()) {
                    JuridiskJob(
                        sykmeldingId = UUID.fromString(rs.getString("sykmelding_id")),
                        status = JuridiskVurderingStatus.valueOf(rs.getString("status")),
                        juridiskVurdering =
                            objectMapper.readValue(rs.getString("juridisk_vurdering")),
                    )
                } else {
                    null
                }
            }
        }
    }

    fun resetHangingJobs(timestamp: OffsetDateTime): Int {
        return transaction {
            JuridiskVurderingTable.update({
                (JuridiskVurderingTable.status inList
                    listOf(
                        JuridiskVurderingStatus.SENDING.name,
                        JuridiskVurderingStatus.FAILED.name,
                    )) and (JuridiskVurderingTable.eventTimestamp less timestamp)
            }) {
                it[status] = JuridiskVurderingStatus.PENDING.name
                it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }
    }
}
