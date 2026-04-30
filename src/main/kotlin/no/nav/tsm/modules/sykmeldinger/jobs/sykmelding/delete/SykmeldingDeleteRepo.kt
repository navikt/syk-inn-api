package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete

import java.time.LocalDate
import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingStatusTable
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskVurderingStatus
import org.jetbrains.exposed.v1.core.*
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.r2dbc.select

class SykmeldingDeleteRepo(val environment: Environment) {
    init {
        require(environment.sykmeldingConfig.retention.inWholeDays >= 1) {
            "Retention to must have at least 1 day, are you trying to test?"
        }
    }

    suspend fun deleteStaleSykmeldinger(): Int = dbQuery {
        // qqaddLogger(StdOutSqlLogger)

        val sykmeldingJoinedWithStatus =
            SykmeldingTable.leftJoin(
                    otherTable = SykmeldingStatusTable,
                    onColumn = { id },
                    otherColumn = { sykmeldingId },
                )
                .leftJoin(
                    otherTable = JuridiskVurderingStatusTable,
                    onColumn = { SykmeldingTable.id },
                    otherColumn = { JuridiskVurderingStatusTable.sykmeldingId },
                )

        val sykmeldingerToBeDeleted =
            sykmeldingJoinedWithStatus.select(SykmeldingTable.id).where {
                (SykmeldingTable.latestTom lessEq cutoff()) and
                    ((JuridiskVurderingStatusTable.status eq JuridiskVurderingStatus.DONE.name) or
                        JuridiskVurderingStatusTable.sykmeldingId.isNull()) and
                    ((SykmeldingStatusTable.status eq SykmeldingStatusStatus.SENT.name) or
                        SykmeldingStatusTable.sykmeldingId.isNull())
            }

        SykmeldingTable.deleteWhere { SykmeldingTable.id inSubQuery sykmeldingerToBeDeleted }
    }

    private fun cutoff() =
        LocalDate.now().minusDays(environment.sykmeldingConfig.retention.inWholeDays)
}
