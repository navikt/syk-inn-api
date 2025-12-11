package no.nav.tsm.syk_inn_api.sykmelding.scheduled

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.UUID
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository

enum class Status {
    PENDING,
    SENDING,
    SENT,
    FAILED
}

data class SykmeldingStatus(
    val sykmeldingId: String,
    val status: Status,
    val mottatt_timestamp: OffsetDateTime,
    val event_timestamp: OffsetDateTime,
)

@Repository
class SykmeldingStatusRepository(
    @param:Autowired private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {

    fun getNextSykmelding(): UUID? {
        return namedParameterJdbcTemplate.query(
            """
         update sykmelding_status ss 
            set status = :statusSending, 
                event_timestamp = now() 
                from (
                    select sykmelding_id from sykmelding_status 
                    where status = :statusPending 
                    order by event_timestamp 
                    FOR UPDATE SKIP LOCKED limit 1) as temp_status
            where ss.sykmelding_id = temp_status.sykmelding_id
            returning ss.sykmelding_id
    """
                .trimIndent(),
            mapOf(
                "statusPending" to Status.PENDING.name,
                "statusSending" to Status.SENDING.name,
            ),
            ResultSetExtractor { rs ->
                if (rs.next()) {
                    rs.getString("sykmelding_id")?.let { UUID.fromString(it) }
                } else {
                    null
                }
            },
        )
    }

    fun getSykmeldingStatus(sykmelding_id: UUID): SykmeldingStatus? {
        return namedParameterJdbcTemplate.query(
            """
                select * from sykmelding_status where sykmelding_id = :sykmelding_id
            """
                .trimIndent(),
            mapOf("sykmelding_id" to sykmelding_id),
            ResultSetExtractor<SykmeldingStatus>() {
                if (it.next()) {
                    SykmeldingStatus(
                        sykmeldingId = it.getString("sykmelding_id"),
                        status = Status.valueOf(it.getString("status")),
                        mottatt_timestamp =
                            it.getTimestamp("mottatt_timestamp").toInstant().atOffset(UTC),
                        event_timestamp =
                            it.getTimestamp("event_timestamp").toInstant().atOffset(UTC)
                    )
                } else {
                    null
                }
            }
        )
    }

    fun updateStatus(sykmeldingId: UUID, status: Status): Int {
        return namedParameterJdbcTemplate.update(
            """
                update sykmelding_status
                    set status = :status, event_timestamp = now()
                where sykmelding_id = :sykmelding_id
            """
                .trimIndent(),
            mapOf("sykmelding_id" to sykmeldingId, "status" to status.name),
        )
    }

    fun insert(sykmeldingId: UUID, mottatt_timestamp: OffsetDateTime): Int {
        return namedParameterJdbcTemplate.update(
            """
                insert into sykmelding_status(
                    sykmelding_id, 
                    status,
                    mottatt_timestamp,
                    event_timestamp
                ) 
                values (:sykmelding_id, :pendingStatus, :mottatt_timestamp, :mottatt_timestamp)
            """
                .trimIndent(),
            mapOf(
                "sykmelding_id" to sykmeldingId,
                "mottatt_timestamp" to Timestamp.from(mottatt_timestamp.toInstant()),
                "pendingStatus" to Status.PENDING.name,
            ),
        )
    }

    fun resetSykmeldingSending(
        timestamp: OffsetDateTime = OffsetDateTime.now().minusMinutes(30)
    ): Int {

        return namedParameterJdbcTemplate.update(
            """
                update sykmelding_status set 
                    status = :pendingStatus,
                    event_timestamp = :timestamp
                where status in (:statusToReset)
                and event_timestamp < :timestamp
            """
                .trimIndent(),
            mapOf(
                "timestamp" to Timestamp.from(timestamp.toInstant()),
                "pendingStatus" to Status.PENDING.name,
                "statusToReset" to listOf(Status.SENDING.name, Status.FAILED.name),
            ),
        )
    }
}
