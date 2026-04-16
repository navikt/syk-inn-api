package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import java.util.UUID
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingInsert
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteWhere

class SykmeldingConsumerRepo : SykmeldingInsert() {
    suspend fun insert(sykmelding: VerifiedSykInnSykmelding): VerifiedSykInnSykmelding = dbQuery {
        insertSykmelding(sykmelding.sykmeldingId, sykmelding)
    }

    suspend fun delete(sykmeldingId: UUID) = dbQuery {
        SykmeldingTable.deleteWhere { SykmeldingTable.id eq sykmeldingId }
    }
}
