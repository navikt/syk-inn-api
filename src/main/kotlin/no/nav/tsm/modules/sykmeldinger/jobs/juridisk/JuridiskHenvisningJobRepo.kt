package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlinx.coroutines.flow.firstOrNull
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingStatusTable
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskVurderingStatus.PENDING
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskVurderingStatus.SENDING
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import org.jetbrains.exposed.v1.core.and
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.core.inSubQuery
import org.jetbrains.exposed.v1.core.less
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption.PostgreSQL.ForUpdate
import org.jetbrains.exposed.v1.core.vendors.ForUpdateOption.PostgreSQL.MODE.SKIP_LOCKED
import org.jetbrains.exposed.v1.r2dbc.select
import org.jetbrains.exposed.v1.r2dbc.update
import org.jetbrains.exposed.v1.r2dbc.updateReturning

data class JuridiskHenvisningJob(
    val sykmeldingId: UUID,
    val status: JuridiskVurderingStatus,
    val juridiskVurdering: List<RegulaJuridiskVurdering>,
)

class JuridiskHenvisningJobRepo {

    @WithSpan
    suspend fun getNext(): JuridiskHenvisningJob? = dbQuery {
        with(JuridiskVurderingStatusTable) {
            updateReturning(
                    listOf(sykmeldingId, status, juridiskVurdering),
                    {
                        sykmeldingId inSubQuery
                            select(sykmeldingId)
                                .where { status eq PENDING.name }
                                .orderBy(eventTimestamp)
                                .limit(1)
                                .forUpdate(ForUpdate(SKIP_LOCKED))
                    },
                ) {
                    it[status] = SENDING.name
                    it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
                }
                .firstOrNull()
                ?.let { row ->
                    JuridiskHenvisningJob(
                        sykmeldingId = row[sykmeldingId],
                        status = JuridiskVurderingStatus.valueOf(row[status]),
                        juridiskVurdering = row[juridiskVurdering],
                    )
                }
        }
    }

    @WithSpan
    suspend fun updateStatus(sykmeldingId: UUID, newStatus: JuridiskVurderingStatus) = dbQuery {
        JuridiskVurderingStatusTable.update({
            JuridiskVurderingStatusTable.sykmeldingId eq sykmeldingId
        }) {
            it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
            it[status] = newStatus.name
        }
    }

    @WithSpan
    suspend fun resetHangingJobs(timestamp: OffsetDateTime): Int {
        return dbQuery {
            JuridiskVurderingStatusTable.update({
                (JuridiskVurderingStatusTable.status inList
                    listOf(SENDING.name, JuridiskVurderingStatus.FAILED.name)) and
                    (JuridiskVurderingStatusTable.eventTimestamp less timestamp)
            }) {
                it[status] = PENDING.name
                it[eventTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
            }
        }
    }
}
