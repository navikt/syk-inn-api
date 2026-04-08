package no.nav.tsm.modules.sykmeldinger.db

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import java.time.OffsetDateTime
import java.util.*
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.db.exposed.JuridiskVurderingTable
import no.nav.tsm.modules.sykmeldinger.db.exposed.SykmeldingTable
import no.nav.tsm.modules.sykmeldinger.db.exposed.fromJsonb.toAktivitetJsonb
import no.nav.tsm.modules.sykmeldinger.db.exposed.fromJsonb.toSykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.db.exposed.fromJsonb.toSykInnArbeidsgiver
import no.nav.tsm.modules.sykmeldinger.db.exposed.fromJsonb.toSykInnDiagnose
import no.nav.tsm.modules.sykmeldinger.db.exposed.fromJsonb.toSykInnMeldinger
import no.nav.tsm.modules.sykmeldinger.db.exposed.fromJsonb.toSykInnResult
import no.nav.tsm.modules.sykmeldinger.db.exposed.fromJsonb.toSykInnTilbakedatering
import no.nav.tsm.modules.sykmeldinger.db.exposed.fromJsonb.toSykInnUtdypendeSporsmal
import no.nav.tsm.modules.sykmeldinger.db.exposed.fromJsonb.toSykInnYrkesskade
import no.nav.tsm.modules.sykmeldinger.db.exposed.toJsonb.toArbeidsgiverJsonb
import no.nav.tsm.modules.sykmeldinger.db.exposed.toJsonb.toDiagnoseJsonb
import no.nav.tsm.modules.sykmeldinger.db.exposed.toJsonb.toMeldingerJsonb
import no.nav.tsm.modules.sykmeldinger.db.exposed.toJsonb.toRuleResultJson
import no.nav.tsm.modules.sykmeldinger.db.exposed.toJsonb.toTilbakedateringJsonb
import no.nav.tsm.modules.sykmeldinger.db.exposed.toJsonb.toUtdypendeSporsmalJsonb
import no.nav.tsm.modules.sykmeldinger.db.exposed.toJsonb.toYrkesskadeJsonb
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingValues
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskVurderingResult
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskVurderingStatus
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.insertReturning
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction
import org.postgresql.util.PSQLException

class SykmeldingRepo {
    private val logger = logger()

    fun insert(
        submitKey: UUID,
        sykmelding: VerifiedSykInnSykmelding,
        juridisk: JuridiskVurderingResult,
    ): Either<String, VerifiedSykInnSykmelding> {
        try {
            val inserted = transaction {
                JuridiskVurderingTable.insert {
                    it[sykmeldingId] = sykmelding.sykmeldingId
                    it[status] = JuridiskVurderingStatus.PENDING.name
                    it[eventTimestamp] = OffsetDateTime.now()
                    it[juridiskVurdering] = juridisk
                }

                SykmeldingTable.insertReturning {
                        it[idempotencyKey] = submitKey
                        it[id] = sykmelding.sykmeldingId
                        it[rules] = sykmelding.result.toRuleResultJson()
                        it[metaSource] = sykmelding.meta.source
                        it[metaMottatt] = sykmelding.meta.mottatt
                        it[metaOrgnummer] = sykmelding.meta.legekontorOrgnr
                        it[metaTelefonnummer] = sykmelding.meta.legekontorTlf
                        it[metaPasientIdent] = sykmelding.meta.pasientIdent
                        it[metaPasientNavn] = sykmelding.meta.pasientNavn
                        it[metaBehandlerNavn] = sykmelding.meta.behandlerNavn
                        it[metaBehandlerHpr] = sykmelding.meta.behandlerHpr
                        it[valuesPasientenSkalSkjermes] = sykmelding.values.pasientenSkalSkjermes
                        it[valuesSvangerskapsrelatert] = sykmelding.values.svangerskapsrelatert
                        it[valuesAnnenFravarsgrunn] = sykmelding.values.annenFravarsgrunn?.name
                        it[valuesHoveddiagnose] = sykmelding.values.hoveddiagnose.toDiagnoseJsonb()
                        it[valuesBidiagnoser] =
                            sykmelding.values.bidiagnoser.mapNotNull { bi -> bi.toDiagnoseJsonb() }
                        it[valuesAktivitet] =
                            sykmelding.values.aktivitet.map { a -> a.toAktivitetJsonb() }
                        it[valuesMeldinger] = sykmelding.values.meldinger.toMeldingerJsonb()
                        it[valuesYrkesskade] = sykmelding.values.yrkesskade.toYrkesskadeJsonb()
                        it[valuesArbeidsgiver] =
                            sykmelding.values.arbeidsgiver.toArbeidsgiverJsonb()
                        it[valuesTilbakedatering] =
                            sykmelding.values.tilbakedatering.toTilbakedateringJsonb()
                        it[valuesUtdypendeSporsmal] =
                            sykmelding.values.utdypendeSporsmal.toUtdypendeSporsmalJsonb()
                    }
                    .single()
                    .sykmeldingRowToVerifiedSykInnSykmelding()
            }

            return inserted.right()
        } catch (e: ExposedSQLException) {
            /** TODO: This cannot possibly be the best way to handle idempotency contraint errors */
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
                    pasientenSkalSkjermes = this[SykmeldingTable.valuesPasientenSkalSkjermes],
                    hoveddiagnose = this[SykmeldingTable.valuesHoveddiagnose]?.toSykInnDiagnose(),
                    bidiagnoser =
                        this[SykmeldingTable.valuesBidiagnoser]?.map { it.toSykInnDiagnose() }
                            ?: emptyList(),
                    aktivitet =
                        this[SykmeldingTable.valuesAktivitet].map { it.toSykInnAktivitet() },
                    svangerskapsrelatert = this[SykmeldingTable.valuesSvangerskapsrelatert],
                    annenFravarsgrunn =
                        this[SykmeldingTable.valuesAnnenFravarsgrunn]?.let {
                            AnnenFravarsgrunn.valueOf(it)
                        },
                    meldinger = this[SykmeldingTable.valuesMeldinger]?.toSykInnMeldinger(),
                    yrkesskade = this[SykmeldingTable.valuesYrkesskade]?.toSykInnYrkesskade(),
                    arbeidsgiver = this[SykmeldingTable.valuesArbeidsgiver]?.toSykInnArbeidsgiver(),
                    tilbakedatering =
                        this[SykmeldingTable.valuesTilbakedatering]?.toSykInnTilbakedatering(),
                    utdypendeSporsmal =
                        this[SykmeldingTable.valuesUtdypendeSporsmal]?.toSykInnUtdypendeSporsmal(),
                ),
            meta =
                SykInnSykmeldingMeta(
                    mottatt = this[SykmeldingTable.metaMottatt],
                    source = this[SykmeldingTable.metaSource],
                    pasientIdent = this[SykmeldingTable.metaPasientIdent],
                    pasientNavn = this[SykmeldingTable.metaPasientNavn],
                    behandlerNavn = this[SykmeldingTable.metaBehandlerNavn],
                    behandlerHpr = this[SykmeldingTable.metaBehandlerHpr],
                    legekontorOrgnr = this[SykmeldingTable.metaOrgnummer],
                    legekontorTlf = this[SykmeldingTable.metaTelefonnummer],
                ),
            result = this[SykmeldingTable.rules].toSykInnResult(),
        )
    }
}
