package no.nav.tsm.syk_inn_api.sykmelding.rules.juridiskvurdering

import java.sql.Timestamp
import java.time.OffsetDateTime
import java.util.*
import no.nav.syfo.rules.juridiskvurdering.JuridiskVurdering
import no.nav.tsm.syk_inn_api.sykmelding.persistence.Status
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.ResultSetExtractor
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Repository
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.cfg.DateTimeFeature
import tools.jackson.module.kotlin.jsonMapper
import tools.jackson.module.kotlin.kotlinModule

data class JuridiskVurderingResult(val juridiskeVurderinger: List<JuridiskVurdering>)

data class NextRuleResult(
    val sykmeldingId: UUID,
    val status: String,
    val juridiskhenvisning: JuridiskVurderingResult
)

@Repository
class JuridiskHenvisningRepository(
    @param:Autowired private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate,
) {

    val objectMapper: ObjectMapper = jsonMapper {
        addModule(kotlinModule())
        disable(DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS)
        disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun insert(
        sykmeldingId: UUID,
        eventTimestamp: OffsetDateTime,
        juridiskHenvisningDB: JuridiskVurderingResult
    ): Int {
        val juridiskhenvisningJson =
            PGobject().apply {
                type = "json"
                value = objectMapper.writeValueAsString(juridiskHenvisningDB)
            }

        return namedParameterJdbcTemplate.update(
            """
                INSERT INTO rule_status(
                    sykmelding_id,
                    status,
                    event_timestamp,
                    juridiskhenvisning
                ) VALUES (
                    :sykmelding_id,
                    :status,
                    :event_timestamp,
                    :juridiskhenvisning
                )
            """
                .trimIndent(),
            mapOf(
                "sykmelding_id" to sykmeldingId,
                "status" to Status.PENDING.name,
                "event_timestamp" to Timestamp.from(eventTimestamp.toInstant()),
                "juridiskhenvisning" to juridiskhenvisningJson,
            ),
        )
    }

    fun resetHangingJobs(timestamp: OffsetDateTime): Int {
        return namedParameterJdbcTemplate.update(
            """
                UPDATE rule_status set
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

    fun getNextToSend(): NextRuleResult? {
        return namedParameterJdbcTemplate.query(
            """
         update rule_status rs 
            set status = :statusSending, 
                event_timestamp = now() 
                from (
                    select sykmelding_id from rule_status 
                    where status = :statusPending 
                    order by event_timestamp
                    FOR UPDATE SKIP LOCKED limit 1) as temp_status
            where rs.sykmelding_id = temp_status.sykmelding_id
            returning rs.sykmelding_id, rs.status, rs.juridiskHenvisning
    """
                .trimIndent(),
            mapOf(
                "statusPending" to Status.PENDING.name,
                "statusSending" to Status.SENDING.name,
            ),
            ResultSetExtractor { rs ->
                if (rs.next()) {
                    NextRuleResult(
                        sykmeldingId = rs.getString("sykmelding_id").let { UUID.fromString(it) },
                        status = rs.getString("status"),
                        juridiskhenvisning =
                            rs.getString("juridiskHenvisning").let {
                                objectMapper.readValue(it, JuridiskVurderingResult::class.java)
                            },
                    )
                } else {
                    null
                }
            },
        )
    }

    fun markAsFailed(nextJuridiskhenvisning: NextRuleResult): Int {
        return updateStatus(nextJuridiskhenvisning, Status.FAILED)
    }

    private fun updateStatus(nextJuridiskhenvisning: NextRuleResult, status: Status): Int =
        namedParameterJdbcTemplate.update(
            """
                    update rule_status 
                        set status = :statusFailed,
                        event_timestamp = now() where sykmelding_id = :sykmeldingId
                """
                .trimIndent(),
            mapOf(
                "statusFailed" to status.name,
                "sykmeldingId" to nextJuridiskhenvisning.sykmeldingId,
            ),
        )

    fun markAsSent(nextJuridiskhenvisning: NextRuleResult): Int {
        return updateStatus(nextJuridiskhenvisning, Status.SENT)
    }
}
