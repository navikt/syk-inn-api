package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete

import java.time.LocalDate
import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingStatusTable
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingStatusTable.sykmeldingId
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable.status
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
        SykmeldingTable.deleteWhere {
            listOf(
                    SykmeldingTable.latestTom lessEq cutoff(),
                    notExists(
                        SykmeldingStatusTable.select(SykmeldingStatusTable.sykmeldingId).where {
                            SykmeldingTable.id eq
                                SykmeldingStatusTable.sykmeldingId and
                                (status neq SykmeldingStatusStatus.SENT.name)
                        }
                    ),
                    notExists(
                        JuridiskVurderingStatusTable.select(sykmeldingId).where {
                            SykmeldingTable.id eq
                                sykmeldingId and
                                (JuridiskVurderingStatusTable.status neq
                                    JuridiskVurderingStatus.DONE.name)
                        }
                    ),
                )
                .compoundAnd()
        }
    }

    private fun cutoff() =
        LocalDate.now().minusDays(environment.sykmeldingConfig.retention.inWholeDays)
}
