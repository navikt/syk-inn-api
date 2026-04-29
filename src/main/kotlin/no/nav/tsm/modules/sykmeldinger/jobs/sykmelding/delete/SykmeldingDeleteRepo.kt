package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete

import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable
import org.jetbrains.exposed.v1.r2dbc.deleteWhere
import org.jetbrains.exposed.v1.core.*
import java.time.LocalDate

class SykmeldingDeleteRepo(val environment: Environment) {
    suspend fun deleteStaleSykmeldinger(): Int = dbQuery {
        SykmeldingTable.deleteWhere {
            SykmeldingTable.latestTom lessEq cutoff()
        }
    }

    private fun cutoff() = LocalDate.now().minusDays(environment.jobs.sykmeldingDeleter.interval.inWholeDays)
}
