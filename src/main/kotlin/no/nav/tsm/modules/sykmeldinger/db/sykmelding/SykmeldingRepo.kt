package no.nav.tsm.modules.sykmeldinger.db.sykmelding

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.r2dbc.postgresql.api.PostgresqlException
import io.r2dbc.spi.R2dbcException
import java.time.OffsetDateTime
import java.util.*
import kotlin.jvm.optionals.getOrNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.toList
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingStatusTable
import no.nav.tsm.modules.sykmeldinger.db.status.ReasonJsonb
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
import no.nav.tsm.modules.sykmeldinger.domain.*
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskVurderingStatus
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn
import org.apache.kafka.shaded.com.google.protobuf.LazyStringArrayList.emptyList
import org.jetbrains.exposed.v1.core.ResultRow
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.ExposedR2dbcException
import org.jetbrains.exposed.v1.r2dbc.R2dbcTransaction
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.insertReturning
import org.jetbrains.exposed.v1.r2dbc.selectAll

abstract class SykmeldingInsert {
    suspend fun R2dbcTransaction.insertSykmelding(
        idempotencyKey: UUID,
        sykmelding: VerifiedSykInnSykmelding,
    ): VerifiedSykInnSykmelding {

        val (behandler, legekontorOrgnr, legekontorTlf) =
            when (sykmelding.meta) {
                is SykInnSykmeldingMeta.Digital ->
                    Triple(
                        sykmelding.meta.behandler,
                        sykmelding.meta.legekontorOrgnr,
                        sykmelding.meta.legekontorTlf,
                    )

                is SykInnSykmeldingMeta.Legacy ->
                    Triple(
                        sykmelding.meta.behandler,
                        sykmelding.meta.legekontorOrgnr,
                        sykmelding.meta.legekontorTlf,
                    )

                is SykInnSykmeldingMeta.Utenlandsk -> Triple(null, null, null)
            }

        return SykmeldingTable.insertReturning {
                it[SykmeldingTable.idempotencyKey] = idempotencyKey
                it[id] = sykmelding.sykmeldingId
                it[type] = sykmelding.type.name
                it[earliestFom] = sykmelding.values.aktivitet.minOf { aktivitet -> aktivitet.fom }
                it[latestTom] = sykmelding.values.aktivitet.maxOf { aktivitet -> aktivitet.tom }
                it[rules] = sykmelding.result.toRuleResultJson()
                it[metaSource] = sykmelding.meta.source
                it[metaMottatt] = sykmelding.meta.mottatt
                it[metaOrgnummer] = legekontorOrgnr
                it[metaTelefonnummer] = legekontorTlf
                it[metaPasientIdent] = sykmelding.meta.pasient.ident
                it[metaPasientNavn] = sykmelding.meta.pasient.toNavnJsonb()
                it[metaBehandlerHpr] = behandler?.hpr
                it[metaBehandlerIdent] = behandler?.ident
                it[metaBehandlerNavn] = behandler?.toNavnJsonb()
                it[metaBehandlerHelsepersonellkategori] = behandler?.helsepersonellkategori
                it[valuesPasientenSkalSkjermes] = sykmelding.values.pasientenSkalSkjermes
                it[valuesSvangerskapsrelatert] = sykmelding.values.svangerskapsrelatert
                it[valuesAnnenFravarsgrunn] = sykmelding.values.annenFravarsgrunn?.name
                it[valuesHoveddiagnose] = sykmelding.values.hoveddiagnose.toDiagnoseJsonb()
                it[valuesBidiagnoser] =
                    sykmelding.values.bidiagnoser.mapNotNull { bi -> bi.toDiagnoseJsonb() }
                it[valuesAktivitet] = sykmelding.values.aktivitet.map { a -> a.toAktivitetJsonb() }
                it[valuesMeldinger] = sykmelding.values.meldinger.toMeldingerJsonb()
                it[valuesYrkesskade] = sykmelding.values.yrkesskade.toYrkesskadeJsonb()
                it[valuesArbeidsgiver] = sykmelding.values.arbeidsgiver.toArbeidsgiverJsonb()
                it[valuesTilbakedatering] =
                    sykmelding.values.tilbakedatering.toTilbakedateringJsonb()
                it[valuesUtdypendeSporsmal] =
                    sykmelding.values.utdypendeSporsmal.toUtdypendeSporsmalJsonb()
            }
            .single()
            .sykmeldingRowToVerifiedSykInnSykmelding()
    }
}

class SykmeldingRepo : SykmeldingInsert() {
    enum class InsertErrors {
        IDEMPOTENCY_HIT
    }

    private val logger = logger()

    suspend fun insert(
        submitKey: UUID,
        sykmelding: VerifiedSykInnSykmelding,
        juridisk: List<RegulaJuridiskVurdering>,
        ruleResult: RegulaResult,
    ): Either<InsertErrors, VerifiedSykInnSykmelding> {
        try {
            val inserted = dbQuery {
                val insertedSykmelding = insertSykmelding(submitKey, sykmelding)

                /**
                 * Status and juridisk has a foreign key constraint to sykmelding, and must be
                 * inserted after.
                 */
                SykmeldingStatusTable.insert {
                    it[sykmeldingId] = sykmelding.sykmeldingId
                    it[status] = SykmeldingStatusStatus.PENDING.name
                    it[reason] =
                        when (ruleResult) {
                            is RegulaResult.Ok -> null
                            is RegulaResult.NotOk ->
                                ruleResult.outcome.reason.let { reason ->
                                    ReasonJsonb(
                                        sykmeldt = reason.sykmeldt,
                                        sykmelder = reason.sykmelder,
                                    )
                                }
                        }
                    it[mottattTimestamp] = sykmelding.meta.mottatt
                    it[eventTimestamp] = OffsetDateTime.now()
                    it[sendTimestamp] = OffsetDateTime.now()
                    it[sourceSystem] = sykmelding.meta.source
                }

                JuridiskVurderingStatusTable.insert {
                    it[sykmeldingId] = sykmelding.sykmeldingId
                    it[status] = JuridiskVurderingStatus.PENDING.name
                    it[eventTimestamp] = OffsetDateTime.now()
                    it[juridiskVurdering] = juridisk
                }

                insertedSykmelding
            }

            return inserted.right()
        } catch (e: ExposedR2dbcException) {
            // TODO: This also catched other violations, for example not-null constraints :/

            val cause = e.cause
            if (cause is PostgresqlException) {
                if (
                    cause.errorDetails.constraintName.getOrNull() ==
                        "sykmelding_idempotency_key_key"
                ) {
                    return InsertErrors.IDEMPOTENCY_HIT.left()
                }
            }

            if (cause is R2dbcException) {
                /**
                 * Parts of the stack trace contains all values, these appear on the second line+
                 */
                val firstLine = e.message.split("\n").firstOrNull() ?: "No message"
                logger.error("Sykmelding insert failed: $firstLine")
                throw IllegalStateException("Sykmelding insert failed: $firstLine")
            }

            throw e
        }
    }

    suspend fun allByIdent(ident: String): List<VerifiedSykInnSykmelding> = dbQuery {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.metaPasientIdent eq ident }
            .map { it.sykmeldingRowToVerifiedSykInnSykmelding() }
            .toList()
    }

    suspend fun byId(sykmeldingId: UUID): VerifiedSykInnSykmelding? = dbQuery {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.id eq sykmeldingId }
            .map { it.sykmeldingRowToVerifiedSykInnSykmelding() }
            .firstOrNull()
    }

    suspend fun byIdempotencyKey(idempotencyKey: UUID): VerifiedSykInnSykmelding? = dbQuery {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.idempotencyKey eq idempotencyKey }
            .map { it.sykmeldingRowToVerifiedSykInnSykmelding() }
            .firstOrNull()
    }
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
                        ?: emptyList<SykInnDiagnoseInfo>(),
                aktivitet = this[SykmeldingTable.valuesAktivitet].map { it.toSykInnAktivitet() },
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
        meta = sykmeldingRowToSykInnSykmeldingMeta(),
        result = this[SykmeldingTable.rules].toSykInnResult(),
        type = SykInnSykmeldingType.valueOf(this[SykmeldingTable.type]),
    )
}

private fun ResultRow.sykmeldingRowToSykInnSykmeldingMeta(): SykInnSykmeldingMeta {
    val navn = this[SykmeldingTable.metaBehandlerNavn]
    val mottatt = this[SykmeldingTable.metaMottatt]
    val source = this[SykmeldingTable.metaSource]
    val hpr = this[SykmeldingTable.metaBehandlerHpr]
    val ident = this[SykmeldingTable.metaBehandlerIdent]
    val helsepersonellkategori = this[SykmeldingTable.metaBehandlerHelsepersonellkategori]
    val legekontorOrgnr = this[SykmeldingTable.metaOrgnummer]
    val legekontorTlf = this[SykmeldingTable.metaTelefonnummer]
    val pasient =
        this[SykmeldingTable.metaPasientNavn].let { navn ->
            SykInnPasient(
                fornavn = navn.fornavn,
                mellomnavn = navn.mellomnavn,
                etternavn = navn.etternavn,
                ident = this[SykmeldingTable.metaPasientIdent],
            )
        }

    return when {
        hpr == null ->
            SykInnSykmeldingMeta.Utenlandsk(source = source, mottatt = mottatt, pasient = pasient)

        source.contains("fhir", ignoreCase = true) &&
            helsepersonellkategori != null &&
            legekontorOrgnr != null &&
            legekontorTlf != null &&
            ident != null &&
            navn != null ->
            SykInnSykmeldingMeta.Digital(
                source = source,
                mottatt = mottatt,
                pasient = pasient,
                behandler =
                    SykInnBehandler(
                        fornavn = navn.fornavn,
                        mellomnavn = navn.mellomnavn,
                        etternavn = navn.etternavn,
                        hpr = hpr,
                        ident = ident,
                        helsepersonellkategori = helsepersonellkategori,
                    ),
                legekontorOrgnr = legekontorOrgnr,
                legekontorTlf = legekontorTlf,
            )

        else -> {
            SykInnSykmeldingMeta.Legacy(
                source = source,
                mottatt = mottatt,
                pasient = pasient,
                SykInnBehandler(
                    fornavn = navn?.fornavn,
                    mellomnavn = navn?.mellomnavn,
                    etternavn = navn?.etternavn,
                    hpr = hpr,
                    ident = ident ?: throw IllegalStateException("Fant ikke fnr"),
                    helsepersonellkategori = helsepersonellkategori ?: emptyList(),
                ),
                legekontorOrgnr = legekontorOrgnr,
                legekontorTlf = legekontorTlf,
            )
        }
    }
}
