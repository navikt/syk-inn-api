package no.nav.tsm.syk_inn_api.sykmelding.scheduled.idepotency

import java.util.UUID
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.core.queryForList
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class IdepotencyUpdater(private val namedParameterJdbcTemplate: NamedParameterJdbcTemplate) {
    private var selectQuery =
        """
        select sykmelding_id from sykmelding where idempotency_key is null limit 1000 for update skip locked
    """
            .trimIndent()

    private var updateQuery =
        """
        update sykmelding set idempotency_key = :idempotencyKey where sykmelding_id = :sykmeldingId
    """
            .trimIndent()

    @Transactional
    fun updateSykmeldinger(): Int {
        val sykmeldinger = namedParameterJdbcTemplate.jdbcTemplate.queryForList<String>(selectQuery)
        val parameters: Array<Map<String, Any>> =
            Array(sykmeldinger.size) {
                mapOf("sykmeldingId" to sykmeldinger[it], "idempotencyKey" to UUID.randomUUID())
            }
        return namedParameterJdbcTemplate.batchUpdate(updateQuery, parameters).sum()
    }
}
