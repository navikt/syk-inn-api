package no.nav.tsm.syk_inn_api.sykmelding

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.sykmelding.response.SykInnArbeidsrelatertArsakType

data class OpprettSykmeldingPayload(
    val meta: OpprettSykmeldingMetadata,
    val values: OpprettSykmelding,
)

data class OpprettSykmeldingMetadata(
    val pasientIdent: String,
    val sykmelderHpr: String,
    val legekontorOrgnr: String,
    val legekontorTlf: String,
)

data class OpprettSykmelding(
    val pasientenSkalSkjermes: Boolean,
    val hoveddiagnose: OpprettSykmeldingDiagnoseInfo,
    val bidiagnoser: List<OpprettSykmeldingDiagnoseInfo>,
    val aktivitet: List<OpprettSykmeldingAktivitet>,
    val meldinger: OpprettSykmeldingMeldinger,
    val svangerskapsrelatert: Boolean,
    val yrkesskade: OpprettSykmeldingYrkesskade?,
    val arbeidsgiver: OpprettSykmeldingArbeidsgiver?,
    val tilbakedatering: OpprettSykmeldingTilbakedatering?,
)

data class OpprettSykmeldingMeldinger(
    val tilNav: String?,
    val tilArbeidsgiver: String?,
)

data class OpprettSykmeldingYrkesskade(
    val yrkesskade: Boolean,
    val skadedato: LocalDate?,
)

data class OpprettSykmeldingArbeidsgiver(
    val harFlere: Boolean,
    val arbeidsgivernavn: String,
)

data class OpprettSykmeldingTilbakedatering(
    val startdato: LocalDate,
    val begrunnelse: String,
)

@JsonSubTypes(
    JsonSubTypes.Type(OpprettSykmeldingAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(OpprettSykmeldingAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(OpprettSykmeldingAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(
        OpprettSykmeldingAktivitet.Behandlingsdager::class,
        name = "BEHANDLINGSDAGER",
    ),
    JsonSubTypes.Type(OpprettSykmeldingAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface OpprettSykmeldingAktivitet {
    data class IkkeMulig(
        val fom: String,
        val tom: String,
        val medisinskArsak: OpprettSykmeldingMedisinskArsak,
        val arbeidsrelatertArsak: OpprettSykmeldingArbeidsrelatertArsak
    ) : OpprettSykmeldingAktivitet

    data class Gradert(
        val grad: Int,
        val fom: String,
        val tom: String,
        val reisetilskudd: Boolean
    ) : OpprettSykmeldingAktivitet

    data class Behandlingsdager(val antallBehandlingsdager: Int, val fom: String, val tom: String) :
        OpprettSykmeldingAktivitet

    data class Avventende(val innspillTilArbeidsgiver: String, val fom: String, val tom: String) :
        OpprettSykmeldingAktivitet

    data class Reisetilskudd(val fom: String, val tom: String) : OpprettSykmeldingAktivitet
}

data class OpprettSykmeldingMedisinskArsak(
    val isMedisinskArsak: Boolean
)

data class OpprettSykmeldingArbeidsrelatertArsak(
    val isArbeidsrelatertArsak: Boolean,
    val arbeidsrelaterteArsaker: List<SykInnArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?
)

data class OpprettSykmeldingDiagnoseInfo(
    val system: DiagnoseSystem,
    val code: String,
)
