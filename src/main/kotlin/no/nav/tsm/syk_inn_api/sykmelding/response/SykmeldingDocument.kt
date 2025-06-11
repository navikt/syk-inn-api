package no.nav.tsm.syk_inn_api.sykmelding.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem

/**
 * The primary data class for exposing to syk-inn through APIs. Also used for other functional
 * features within syk-inn-api, such as PDF generation.
 */
data class SykmeldingDocument(
    val sykmeldingId: String,
    val meta: SykmeldingDocumentMeta,
    val values: SykmeldingDocumentValues,
)

data class SykmeldingDocumentMeta(
    val pasientIdent: String,
    val sykmelderHpr: String,
    val legekontorOrgnr: String,
)

data class SykmeldingDocumentValues(
    val hoveddiagnose: SykmeldingDocumentDiagnoseInfo?,
    val aktivitet: List<SykmeldingDocumentAktivitet>,
    val bidiagnoser: List<SykmeldingDocumentDiagnoseInfo>,
    val svangerskapsrelatert: Boolean,
    val pasientenSkalSkjermes: Boolean,
    val meldinger: SykmeldingDocumentgMeldinger,
    val yrkesskade: SykmeldingDocumentYrkesskade?,
    val arbeidsgiver: SykmeldingDocumentArbeidsgiver?,
    val tilbakedatering: SykmeldingDocumentTilbakedatering?,
    val regelResultat: SykmeldingDocumentRuleResult,
)

@JsonSubTypes(
    JsonSubTypes.Type(SykmeldingDocumentAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(SykmeldingDocumentAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(SykmeldingDocumentAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(
        SykmeldingDocumentAktivitet.Behandlingsdager::class,
        name = "BEHANDLINGSDAGER",
    ),
    JsonSubTypes.Type(SykmeldingDocumentAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface SykmeldingDocumentAktivitet {
    data class IkkeMulig(val fom: String, val tom: String) : SykmeldingDocumentAktivitet

    data class Gradert(
        val grad: Int,
        val fom: String,
        val tom: String,
        val reisetilskudd: Boolean
    ) : SykmeldingDocumentAktivitet

    data class Behandlingsdager(val antallBehandlingsdager: Int, val fom: String, val tom: String) :
        SykmeldingDocumentAktivitet

    data class Avventende(val innspillTilArbeidsgiver: String, val fom: String, val tom: String) :
        SykmeldingDocumentAktivitet

    data class Reisetilskudd(val fom: String, val tom: String) : SykmeldingDocumentAktivitet
}

data class SykmeldingDocumentDiagnoseInfo(
    val system: DiagnoseSystem,
    val code: String,
    val text: String,
)

data class SykmeldingDocumentRuleResult(
    val result: String,
    val melding: String?,
)

data class SykmeldingDocumentTilbakedatering(
    val startdato: LocalDate,
    val begrunnelse: String,
)

data class SykmeldingDocumentArbeidsgiver(
    val harFlere: Boolean,
    val arbeidsgivernavn: String,
)

data class SykmeldingDocumentYrkesskade(
    val yrkesskade: Boolean,
    val skadedato: LocalDate?,
)

data class SykmeldingDocumentgMeldinger(
    val tilNav: String?,
    val tilArbeidsgiver: String?,
)
