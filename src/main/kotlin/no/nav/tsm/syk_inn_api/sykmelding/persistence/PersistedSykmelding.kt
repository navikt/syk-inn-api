package no.nav.tsm.syk_inn_api.sykmelding.persistence

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.LocalDateTime
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.person.Navn
import no.nav.tsm.syk_inn_api.sykmelding.response.SykInnArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.RuleType

data class PersistedSykmelding(
    val sykmeldingId: String,
    val pasient: PersistedSykmeldingPasient,
    val sykmelder: PersistedSykmeldingSykmelder,
    val hoveddiagnose: PersistedSykmeldingDiagnoseInfo?,
    val bidiagnoser: List<PersistedSykmeldingDiagnoseInfo>,
    val aktivitet: List<PersistedSykmeldingAktivitet>,
    val svangerskapsrelatert: Boolean,
    val pasientenSkalSkjermes: Boolean,
    val meldinger: PersistedSykmeldingMeldinger,
    val yrkesskade: PersistedSykmeldingYrkesskade?,
    val arbeidsgiver: PersistedSykmeldingArbeidsgiver?,
    val tilbakedatering: PersistedSykmeldingTilbakedatering?,
    val regelResultat: PersistedSykmeldingRuleResult,
)

data class PersistedSykmeldingRuleResult(
    val result: RuleType,
    val meldingTilSender: String?,
)

data class PersistedSykmeldingSykmelder(
    val godkjenninger: List<PersistedSykmeldingHprGodkjenning> = emptyList(),
    val ident: String,
    val hprNummer: String,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

data class PersistedSykmeldingPasient(
    val navn: Navn,
    val ident: String,
    val fodselsdato: LocalDate,
)

data class PersistedSykmeldingMeldinger(
    val tilNav: String?,
    val tilArbeidsgiver: String?,
)

data class PersistedSykmeldingYrkesskade(
    val yrkesskade: Boolean,
    val skadedato: LocalDate?,
)

data class PersistedSykmeldingArbeidsgiver(
    val harFlere: Boolean,
    val arbeidsgivernavn: String,
)

data class PersistedSykmeldingTilbakedatering(
    val startdato: LocalDate,
    val begrunnelse: String,
)

@JsonSubTypes(
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(
        PersistedSykmeldingAktivitet.Behandlingsdager::class,
        name = "BEHANDLINGSDAGER"
    ),
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface PersistedSykmeldingAktivitet {
    data class IkkeMulig(
        val fom: LocalDate,
        val tom: LocalDate,
        val medisinskArsak: PersistedSykmeldingMedisinskArsak,
        val arbeidsrelatertArsak: PersistedSykmeldingArbeidsrelatertArsak
    ) : PersistedSykmeldingAktivitet

    data class Gradert(
        val grad: Int,
        val fom: LocalDate,
        val tom: LocalDate,
        val reisetilskudd: Boolean
    ) : PersistedSykmeldingAktivitet

    data class Behandlingsdager(
        val antallBehandlingsdager: Int,
        val fom: LocalDate,
        val tom: LocalDate
    ) : PersistedSykmeldingAktivitet

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        val fom: LocalDate,
        val tom: LocalDate
    ) : PersistedSykmeldingAktivitet

    data class Reisetilskudd(val fom: LocalDate, val tom: LocalDate) : PersistedSykmeldingAktivitet
}

data class PersistedSykmeldingMedisinskArsak(val isMedisinskArsak: Boolean)

data class PersistedSykmeldingArbeidsrelatertArsak(
    val isArbeidsrelatertArsak: Boolean,
    val arbeidsrelaterteArsaker: List<SykInnArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?,
)

data class PersistedSykmeldingDiagnoseInfo(
    val system: DiagnoseSystem,
    val code: String,
    val text: String,
)

data class PersistedSykmeldingHprGodkjenning(
    val helsepersonellkategori: PersistedSykmeldingHprKode? = null,
    val autorisasjon: PersistedSykmeldingHprKode? = null,
    val tillegskompetanse: List<PersistedSykmeldingHprTilleggskompetanse>? = null,
)

data class PersistedSykmeldingHprTilleggskompetanse(
    val avsluttetStatus: PersistedSykmeldingHprKode?,
    val eTag: String?,
    val gyldig: PersistedSykmeldingHprGyldighetsPeriode?,
    val id: Int?,
    val type: PersistedSykmeldingHprKode?
)

data class PersistedSykmeldingHprGyldighetsPeriode(
    val fra: LocalDateTime?,
    val til: LocalDateTime?
)

data class PersistedSykmeldingHprKode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?,
)
