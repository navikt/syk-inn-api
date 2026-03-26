@file:OptIn(ExperimentalUuidApi::class)

package no.nav.tsm.modules.sykmeldinger.db

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.collections.emptyList
import kotlin.uuid.ExperimentalUuidApi
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.db.exposed.SykmeldingTable
import no.nav.tsm.modules.sykmeldinger.db.exposed.toRuleResultColumn
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingValues
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.postgresql.util.PSQLException

class SykmeldingRepo {
    private val logger = logger()

    fun insert(submitKey: UUID, sykmelding: VerifiedSykInnSykmelding): Either<String, Boolean> {
        try {
            transaction {
                SykmeldingTable.insert {
                    it[idempotencyKey] = submitKey
                    it[id] = sykmelding.sykmeldingId
                    it[rules] = sykmelding.result.toRuleResultColumn()
                    it[metaSource] = sykmelding.meta.source
                    it[metaMottatt] = sykmelding.meta.mottatt
                    it[metaOrgnummer] = sykmelding.meta.legekontorOrgnr
                    it[metaTelefonnummer] = sykmelding.meta.legekontorTlf
                    it[metaPasientIdent] = sykmelding.meta.pasientIdent
                    it[metaBehandlerNavn] = sykmelding.meta.behandlerNavn
                    it[metaBehandlerHpr] = sykmelding.meta.behandlerHpr
                    it[valuesPasientenSkalSkjermes] = sykmelding.values.pasientenSkalSkjermes
                    it[valuesSvangerskapsrelatert] = sykmelding.values.svangerskapsrelatert
                    it[valuesHoveddiagnose] = null
                    it[valuesBidiagnoser] = "[]"
                    it[valuesAktivitet] = "[]"
                    it[valuesMeldinger] = null
                    it[valuesYrkesskade] = null
                    it[valuesArbeidsgiver] = null
                    it[valuesTilbakedatering] = null
                    it[valuesUtdypendeSporsmal] = null
                    it[valuesAnnenFravarsgrunn] = null
                }
            }
            return true.right()
        } catch (e: ExposedSQLException) {
            if (
                e.message?.contains(
                    """violates unique constraint "sykmelding_idempotency_key_key""""
                ) == true
            ) {
                return "Idempotency Key triggered".left()
            }

            if (e.cause is PSQLException) {
                /**
                 * Parts of the stack trace contains all values, these appear on the second line+
                 */
                val firstLine = e.message?.split("\n")?.firstOrNull() ?: "No message"
                logger.error("Sykmelding insert failed: $firstLine")
                throw IllegalStateException("Sykmelding insert failed: ${firstLine}")
            }

            throw e
        }
    }

    fun allByIdent(ident: String): List<VerifiedSykInnSykmelding> = transaction {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.metaPasientIdent eq ident }
            .map { it.sykmeldingRowToVerifiedSykInnSykmelding() }
    }

    fun byId(sykmeldingId: UUID): VerifiedSykInnSykmelding? = transaction {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.id eq sykmeldingId }
            .map { it.sykmeldingRowToVerifiedSykInnSykmelding() }
            .firstOrNull()
    }

    fun byIdempotencyKey(idempotencyKey: UUID): VerifiedSykInnSykmelding? = transaction {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.idempotencyKey eq idempotencyKey }
            .map { it.sykmeldingRowToVerifiedSykInnSykmelding() }
            .firstOrNull()
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
                    source = "tihi",
                    mottatt = OffsetDateTime.now(),
                    pasientIdent = "",
                    // TODO
                    behandlerNavn = this[SykmeldingTable.metaBehandlerNavn],
                    behandlerHpr = this[SykmeldingTable.metaBehandlerHpr],
                    legekontorOrgnr = "",
                    legekontorTlf = "",
                ),
            result = SykInnSykmeldingRuleResult.OK(),
        )
    }
}
