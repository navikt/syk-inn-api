package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete

import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.core.Environment
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.core.utils.sykmeldingCutoffDate
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
    @WithSpan
    suspend fun deleteStaleSykmeldinger(): Int = dbQuery {
        SykmeldingTable.deleteWhere {
            listOf(
                    SykmeldingTable.latestTom lessEq environment.sykmeldingCutoffDate(),
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
}
