package no.nav.tsm.modules.sykmeldinger.db.sykmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.r2dbc.spi.R2dbcException
import java.time.OffsetDateTime
import java.util.UUID
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingTable
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toAktivitetJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toSykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toSykInnArbeidsgiver
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toSykInnDiagnose
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toSykInnMeldinger
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toSykInnResult
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toSykInnTilbakedatering
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toSykInnUtdypendeSporsmal
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toSykInnYrkesskade
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toArbeidsgiverJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toDiagnoseJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toMeldingerJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toNavnJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toRuleResultJson
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toTilbakedateringJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toUtdypendeSporsmalJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toYrkesskadeJsonb
import no.nav.tsm.modules.sykmeldinger.domain.SykInnBehandler
import no.nav.tsm.modules.sykmeldinger.domain.SykInnPasient
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingValues
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskVurderingResult
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskVurderingStatus
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.exceptions.ExposedSQLException
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.insertReturning
import org.jetbrains.exposed.v1.r2dbc.selectAll
import org.jetbrains.exposed.v1.r2dbc.transactions.suspendTransaction

class SykmeldingRepo {
    private val logger = logger()

    suspend fun insert(
        submitKey: UUID,
        sykmelding: VerifiedSykInnSykmelding,
        juridisk: JuridiskVurderingResult,
    ): Either<String, VerifiedSykInnSykmelding> {
        try {
            val inserted = suspendTransaction {
                JuridiskVurderingTable.insert {
                    it[sykmeldingId] = sykmelding.sykmeldingId
                    it[status] = JuridiskVurderingStatus.PENDING.name
                    it[eventTimestamp] = OffsetDateTime.now()
                    it[juridiskVurdering] = juridisk
                }

                SykmeldingStatusTable.insert {
                    it[sykmeldingId] = sykmelding.sykmeldingId
                    it[status] = SykmeldingStatusStatus.PENDING.name
                    it[mottattTimestamp] = sykmelding.meta.mottatt
                    it[eventTimestamp] = OffsetDateTime.now()
                    it[sendTimestamp] = OffsetDateTime.now()
                    it[sourceSystem] = sykmelding.meta.source
                }

                SykmeldingTable.insertReturning {
                        it[idempotencyKey] = submitKey
                        it[id] = sykmelding.sykmeldingId
                        it[rules] = sykmelding.result.toRuleResultJson()
                        it[metaSource] = sykmelding.meta.source
                        it[metaMottatt] = sykmelding.meta.mottatt
                        it[metaOrgnummer] = sykmelding.meta.legekontorOrgnr
                        it[metaTelefonnummer] = sykmelding.meta.legekontorTlf
                        it[metaPasientIdent] = sykmelding.meta.pasient.ident
                        it[metaPasientNavn] = sykmelding.meta.pasient.toNavnJsonb()
                        it[metaBehandlerHpr] = sykmelding.meta.behandler.hpr
                        it[metaBehandlerNavn] = sykmelding.meta.behandler.toNavnJsonb()
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

            if (e.cause is R2dbcException) {
                /**
                 * Parts of the stack trace contains all values, these appear on the second line+
                 */
                val firstLine = e.message?.split("\n")?.firstOrNull() ?: "No message"
                logger.error("Sykmelding insert failed: $firstLine")
                throw IllegalStateException("Sykmelding insert failed: $firstLine")
            }

            throw e
        }
    }

    suspend fun allByIdent(ident: String): List<VerifiedSykInnSykmelding> = suspendTransaction {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.metaPasientIdent eq ident }
            .map { it.sykmeldingRowToVerifiedSykInnSykmelding() }
            .toList()
    }

    suspend fun byId(sykmeldingId: UUID): VerifiedSykInnSykmelding? = suspendTransaction {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.id eq sykmeldingId }
            .map { it.sykmeldingRowToVerifiedSykInnSykmelding() }
            .firstOrNull()
    }

    suspend fun byIdempotencyKey(idempotencyKey: UUID): VerifiedSykInnSykmelding? =
        suspendTransaction {
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
                    pasient =
                        this[SykmeldingTable.metaPasientNavn].let { navn ->
                            SykInnPasient(
                                fornavn = navn.fornavn,
                                mellomnavn = navn.mellomnavn,
                                etternavn = navn.etternavn,
                                ident = this[SykmeldingTable.metaPasientIdent],
                            )
                        },
                    behandler =
                        this[SykmeldingTable.metaBehandlerNavn].let { navn ->
                            SykInnBehandler(
                                fornavn = navn.fornavn,
                                mellomnavn = navn.mellomnavn,
                                etternavn = navn.etternavn,
                                hpr = this[SykmeldingTable.metaBehandlerHpr],
                            )
                        },
                    legekontorOrgnr = this[SykmeldingTable.metaOrgnummer],
                    legekontorTlf = this[SykmeldingTable.metaTelefonnummer],
                ),
            result = this[SykmeldingTable.rules].toSykInnResult(),
        )
    }
}
