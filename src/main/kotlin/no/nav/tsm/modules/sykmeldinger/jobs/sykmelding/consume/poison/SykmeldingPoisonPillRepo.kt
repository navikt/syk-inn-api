package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.poison

import java.util.UUID
import kotlinx.coroutines.flow.firstOrNull
import no.nav.tsm.core.db.dbQuery
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.selectAll

class SykmeldingPoisonPillRepo {
    suspend fun isPoisoned(sykmeldingId: UUID): PoisonedSykmelding? = dbQuery {
        val result =
            SykmeldingPoisonPillTable.selectAll()
                .where { SykmeldingPoisonPillTable.sykmeldingId eq sykmeldingId }
                .firstOrNull()

        if (result == null) null
        else {
            PoisonedSykmelding(
                id = sykmeldingId,
                reason = result[SykmeldingPoisonPillTable.reason],
                created = result[SykmeldingPoisonPillTable.created],
            )
        }
    }
}
