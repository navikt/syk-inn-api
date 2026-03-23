@file:OptIn(ExperimentalUuidApi::class)

package no.nav.tsm.modules.sykmeldinger.db

import java.time.OffsetDateTime
import kotlin.time.Clock
import kotlin.uuid.ExperimentalUuidApi
import no.nav.tsm.modules.sykmeldinger.db.exposed.SykmeldingJsonb
import no.nav.tsm.modules.sykmeldinger.db.exposed.SykmeldingTable
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingValues
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SykmeldingerRepo() {
    fun insertSykmelding(sykmelding: VerifiedSykInnSykmelding) {
        transaction {
            SykmeldingTable.insert {
                it[id] = sykmelding.sykmeldingId
                it[createdAt] = Clock.System.now()
                it[updatedAt] = Clock.System.now()
                it[pasientIdent] = sykmelding.meta.pasientIdent
                it[data] = SykmeldingJsonb(sykmeldingId = sykmelding.sykmeldingId.toString())
            }
        }
    }

    fun sykmeldinger(ident: String): List<VerifiedSykInnSykmelding> = transaction {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.pasientIdent eq ident }
            .map { it.sykmeldingRowToVerifiedSykInnSykmelding() }
    }

    private fun ResultRow.sykmeldingRowToVerifiedSykInnSykmelding(): VerifiedSykInnSykmelding {
        return VerifiedSykInnSykmelding(
            sykmeldingId = this[SykmeldingTable.id],
            values =
                SykInnSykmeldingValues(
                    pasientenSkalSkjermes = false,
                    hoveddiagnose = null,
                    bidiagnoser = emptyList(),
                    aktivitet = emptyList(),
                    svangerskapsrelatert = false,
                    meldinger = null,
                    yrkesskade = null,
                    arbeidsgiver = null,
                    tilbakedatering = null,
                    utdypendeSporsmal = null,
                    annenFravarsgrunn = null,
                ),
            meta =
                SykInnSykmeldingMeta(
                    mottatt = OffsetDateTime.now(),
                    pasientIdent = "",
                    hpr = "",
                    legekontorOrgnr = "",
                    legekontorTlf = "",
                ),
            result = SykInnSykmeldingRuleResult.OK(),
        )
    }
}
