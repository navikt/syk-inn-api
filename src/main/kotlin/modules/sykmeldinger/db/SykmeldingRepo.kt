@file:OptIn(ExperimentalUuidApi::class)

package modules.sykmeldinger.db

import java.util.UUID
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import modules.sykmeldinger.db.exposed.SykmeldingJsonb
import modules.sykmeldinger.db.exposed.SykmeldingTable
import modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.lessEq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SykmeldingerRepo() {
    fun insertSykmelding(sykmelding: VerifiedSykInnSykmelding) {
        transaction {
            SykmeldingTable.insert {
                it[id] = sykmelding.sykmeldingId
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
                it[data] = SykmeldingJsonb(sykmeldingId = sykmelding.sykmeldingId.toString())
            }
        }
    }

    fun sykmeldinger(ident: String): List<VerifiedSykInnSykmelding> = transaction {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.pasientIdent eq ident }
            .map {
                it[SykmeldingTable.data]
                // her må det mappes over en lav sko

                VerifiedSykInnSykmelding(
                    sykmeldingId = it[SykmeldingTable.id],
                    values = TODO("tihi"),
                    meta = TODO("tihi"),
                    result = TODO("tihi"),
                )
            }
    }
}
