package no.nav.tsm.syk_inn_api.sykmelding.persistence

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

data class NextSykmelding(
    val sykmeldingId: UUID,
    val source: String,
)

@Repository
class SykmeldingStatusRepository(
    @param:Autowired private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate
) {

    fun getNextSykmelding(): NextSykmelding? {
        return namedParameterJdbcTemplate.query(
            """
         update sykmelding_status ss 
            set status = :statusSending, 
                event_timestamp = now() 
                from (
                    select sykmelding_id from sykmelding_status 
                    where status = :statusPending 
                    and send_timestamp < now()
                    order by send_timestamp 
                    FOR UPDATE SKIP LOCKED limit 1) as temp_status
            where ss.sykmelding_id = temp_status.sykmelding_id
            returning ss.sykmelding_id, ss.source
    """
                .trimIndent(),
            mapOf(
                "statusPending" to Status.PENDING.name,
                "statusSending" to Status.SENDING.name,
            ),
            ResultSetExtractor { rs ->
                if (rs.next()) {
                    NextSykmelding(
                        sykmeldingId = rs.getString("sykmelding_id").let { UUID.fromString(it) },
                        source = rs.getString("source")
                    )
                } else {
                    null
                }
            },
        )
    }

    fun getSykmeldingStatus(sykmeldingId: UUID): SykmeldingStatus? {
        return namedParameterJdbcTemplate.query(
            """
                select * from sykmelding_status where sykmelding_id = :sykmelding_id
            """
                .trimIndent(),
            mapOf("sykmelding_id" to sykmeldingId),
            ResultSetExtractor {
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

    fun insert(
        sykmeldingId: UUID,
        mottattTimestamp: OffsetDateTime,
        sendTimestamp: OffsetDateTime,
        source: String,
    ): Int {
        return namedParameterJdbcTemplate.update(
            """
                insert into sykmelding_status(
                    sykmelding_id, 
                    status,
                    mottatt_timestamp,
                    event_timestamp,
                    send_timestamp,
                    source
                ) 
                values (:sykmelding_id, :pendingStatus, :mottatt_timestamp, :mottatt_timestamp, :send_timestamp, :source)
            """
                .trimIndent(),
            mapOf(
                "sykmelding_id" to sykmeldingId,
                "mottatt_timestamp" to Timestamp.from(mottattTimestamp.toInstant()),
                "pendingStatus" to Status.PENDING.name,
                "send_timestamp" to Timestamp.from(sendTimestamp.toInstant()),
                "source" to source
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
                    event_timestamp = now()
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
