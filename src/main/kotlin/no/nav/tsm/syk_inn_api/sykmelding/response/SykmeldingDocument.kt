package no.nav.tsm.syk_inn_api.sykmelding.response

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.time.OffsetDateTime
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem

/**
 * The primary data class for exposing to syk-inn through APIs. Also used for other functional
 * features within syk-inn-api, such as PDF generation.
 */
data class SykmeldingDocument(
    val sykmeldingId: String,
    val meta: SykmeldingDocumentMeta,
    val values: SykmeldingDocumentValues,
    val utfall: SykmeldingDocumentRuleResult,
)

data class SykmeldingDocumentMeta(
    val mottatt: OffsetDateTime,
    val pasientIdent: String,
    val sykmelderHpr: String,
    val legekontorOrgnr: String?,
)

data class SykmeldingDocumentValues(
    val hoveddiagnose: SykmeldingDocumentDiagnoseInfo?,
    val bidiagnoser: List<SykmeldingDocumentDiagnoseInfo>?,
    val aktivitet: List<SykmeldingDocumentAktivitet>,
    val svangerskapsrelatert: Boolean,
    val pasientenSkalSkjermes: Boolean,
    val meldinger: SykmeldingDocumentMeldinger,
    val yrkesskade: SykmeldingDocumentYrkesskade?,
    val arbeidsgiver: SykmeldingDocumentArbeidsgiver?,
    val tilbakedatering: SykmeldingDocumentTilbakedatering?,
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
sealed class SykmeldingDocumentAktivitet(open val fom: String, open val tom: String) {
    data class IkkeMulig(
        override val fom: String,
        override val tom: String,
    ) : SykmeldingDocumentAktivitet(fom, tom)

    data class Gradert(
        override val fom: String,
        override val tom: String,
        val grad: Int,
        val reisetilskudd: Boolean
    ) : SykmeldingDocumentAktivitet(fom, tom)

    data class Behandlingsdager(
        override val fom: String,
        override val tom: String,
        val antallBehandlingsdager: Int
    ) : SykmeldingDocumentAktivitet(fom, tom)

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: String,
        override val tom: String,
    ) : SykmeldingDocumentAktivitet(fom, tom)

    data class Reisetilskudd(
        override val fom: String,
        override val tom: String,
    ) : SykmeldingDocumentAktivitet(fom, tom)
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

data class SykmeldingDocumentMeldinger(
    val tilNav: String?,
    val tilArbeidsgiver: String?,
)
