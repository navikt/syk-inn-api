package no.nav.tsm.syk_inn_api.sykmelding.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem

data class SykmeldingDocument(
    val sykmeldingId: String,
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: ExistingSykmelding,
    val legekontorOrgnr: String,
)

data class ExistingSykmelding(
    val hoveddiagnose: ExistingSykmeldingDiagnoseInfo?,
    val aktivitet: List<ExistingSykmeldingAktivitet>,
    val bidiagnoser: List<ExistingSykmeldingDiagnoseInfo>,
    val svangerskapsrelatert: Boolean,
    val pasientenSkalSkjermes: Boolean,
    val meldinger: ExistingSykmeldingMeldinger,
    val yrkesskade: ExistingSykmeldingYrkesskade?,
    val arbeidsgiver: ExistingSykmeldingArbeidsgiver?,
    val tilbakedatering: ExistingSykmeldingTilbakedatering?,
    val regelResultat: ExistingSykmeldingRuleResult,
)

@JsonSubTypes(
    JsonSubTypes.Type(ExistingSykmeldingAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(ExistingSykmeldingAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(ExistingSykmeldingAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(
        ExistingSykmeldingAktivitet.Behandlingsdager::class,
        name = "BEHANDLINGSDAGER"
    ),
    JsonSubTypes.Type(ExistingSykmeldingAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface ExistingSykmeldingAktivitet {
    data class IkkeMulig(val fom: String, val tom: String) : ExistingSykmeldingAktivitet

    data class Gradert(
        val grad: Int,
        val fom: String,
        val tom: String,
        val reisetilskudd: Boolean
    ) : ExistingSykmeldingAktivitet

    data class Behandlingsdager(val antallBehandlingsdager: Int, val fom: String, val tom: String) :
        ExistingSykmeldingAktivitet

    data class Avventende(val innspillTilArbeidsgiver: String, val fom: String, val tom: String) :
        ExistingSykmeldingAktivitet

    data class Reisetilskudd(val fom: String, val tom: String) : ExistingSykmeldingAktivitet
}

data class ExistingSykmeldingDiagnoseInfo(
    val system: DiagnoseSystem,
    val code: String,
    val text: String,
)

data class ExistingSykmeldingRuleResult(
    val result: String,
    val melding: String?,
)

data class ExistingSykmeldingTilbakedatering(
    val startdato: LocalDate,
    val begrunnelse: String,
)

data class ExistingSykmeldingArbeidsgiver(
    val harFlere: Boolean,
    val arbeidsgivernavn: String,
)

data class ExistingSykmeldingYrkesskade(
    val yrkesskade: Boolean,
    val skadedato: LocalDate?,
)

data class ExistingSykmeldingMeldinger(
    val tilNav: String?,
    val tilArbeidsgiver: String?,
)
