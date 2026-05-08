package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import java.util.*
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.FromJsonb.toAktivitetJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingInsert
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.earliestFom
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.latestTom
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaBehandlerHelsepersonellkategori
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaBehandlerHpr
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaBehandlerIdent
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaBehandlerNavn
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaMottatt
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaOrgnummer
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaPasientIdent
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaPasientNavn
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaSource
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.metaTelefonnummer
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.rules
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesAktivitet
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesAnnenFravarsgrunn
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesArbeidsgiver
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesBidiagnoser
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesHoveddiagnose
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesMeldinger
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesPasientenSkalSkjermes
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesSvangerskapsrelatert
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesTilbakedatering
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesUtdypendeSporsmal
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable.valuesYrkesskade
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toArbeidsgiverJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toDiagnoseJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toMeldingerJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toNavnJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toRuleResultJson
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toTilbakedateringJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toUtdypendeSporsmalJsonb
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.ToJsonb.toYrkesskadeJsonb
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.core.inList
import org.jetbrains.exposed.v1.r2dbc.batchUpsert
import org.jetbrains.exposed.v1.r2dbc.deleteWhere

class SykmeldingConsumerRepo : SykmeldingInsert() {
    suspend fun insert(sykmelding: VerifiedSykInnSykmelding): VerifiedSykInnSykmelding = dbQuery {
        insertSykmelding(sykmelding.sykmeldingId, sykmelding)
    }

    suspend fun delete(sykmeldingId: UUID) = dbQuery {
        SykmeldingTable.deleteWhere { SykmeldingTable.id eq sykmeldingId }
    }

    suspend fun batchDelete(sykmeldingIds: List<UUID>) = dbQuery {
        SykmeldingTable.deleteWhere { SykmeldingTable.id inList sykmeldingIds }
    }

    suspend fun batchInsert(sykmeldinger: List<VerifiedSykInnSykmelding>) = dbQuery {
        SykmeldingTable.batchUpsert(sykmeldinger) { sykmelding ->
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
            this[SykmeldingTable.idempotencyKey] = sykmelding.sykmeldingId
            this[SykmeldingTable.id] = sykmelding.sykmeldingId
            this[SykmeldingTable.type] = sykmelding.type.name
            this[earliestFom] = sykmelding.values.aktivitet.minOf { aktivitet -> aktivitet.fom }
            this[latestTom] = sykmelding.values.aktivitet.maxOf { aktivitet -> aktivitet.tom }
            this[rules] = sykmelding.result.toRuleResultJson()
            this[metaSource] = sykmelding.meta.source
            this[metaMottatt] = sykmelding.meta.mottatt
            this[metaOrgnummer] = legekontorOrgnr
            this[metaTelefonnummer] = legekontorTlf
            this[metaPasientIdent] = sykmelding.meta.pasient.ident
            this[metaPasientNavn] = sykmelding.meta.pasient.toNavnJsonb()
            this[metaBehandlerHpr] = behandler?.hpr
            this[metaBehandlerIdent] = behandler?.ident
            this[metaBehandlerNavn] = behandler?.toNavnJsonb()
            this[metaBehandlerHelsepersonellkategori] = behandler?.helsepersonellkategori
            this[valuesPasientenSkalSkjermes] = sykmelding.values.pasientenSkalSkjermes
            this[valuesSvangerskapsrelatert] = sykmelding.values.svangerskapsrelatert
            this[valuesAnnenFravarsgrunn] = sykmelding.values.annenFravarsgrunn?.name
            this[valuesHoveddiagnose] = sykmelding.values.hoveddiagnose.toDiagnoseJsonb()
            this[valuesBidiagnoser] =
                sykmelding.values.bidiagnoser.mapNotNull { bi -> bi.toDiagnoseJsonb() }
            this[valuesAktivitet] = sykmelding.values.aktivitet.map { a -> a.toAktivitetJsonb() }
            this[valuesMeldinger] = sykmelding.values.meldinger.toMeldingerJsonb()
            this[valuesYrkesskade] = sykmelding.values.yrkesskade.toYrkesskadeJsonb()
            this[valuesArbeidsgiver] = sykmelding.values.arbeidsgiver.toArbeidsgiverJsonb()
            this[valuesTilbakedatering] = sykmelding.values.tilbakedatering.toTilbakedateringJsonb()
            this[valuesUtdypendeSporsmal] =
                sykmelding.values.utdypendeSporsmal.toUtdypendeSporsmalJsonb()
        }
    }
}
