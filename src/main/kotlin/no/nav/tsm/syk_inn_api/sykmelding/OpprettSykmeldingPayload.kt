package no.nav.tsm.syk_inn_api.sykmelding

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate
import java.util.UUID
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.sykmelding.response.SykInnArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn

data class OpprettSykmeldingPayload(
    val submitId: UUID,
    val meta: OpprettSykmeldingMetadata,
    val values: OpprettSykmelding,
)

data class OpprettSykmeldingMetadata(
    val source: String,
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
    val utdypendeSporsmal: OpprettSykmeldingUtdypendeSporsmal?,
    val annenFravarsgrunn: AnnenFravarsgrunn?
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

data class OpprettSykmeldingUtdypendeSporsmal(
    val hensynPaArbeidsplassen: String?,
    val medisinskOppsummering: String?,
    val utfordringerMedArbeid: String?,
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

    val fom: LocalDate
    val tom: LocalDate

    data class IkkeMulig(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val medisinskArsak: OpprettSykmeldingMedisinskArsak,
        val arbeidsrelatertArsak: OpprettSykmeldingArbeidsrelatertArsak
    ) : OpprettSykmeldingAktivitet

    data class Gradert(
        val grad: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
        val reisetilskudd: Boolean
    ) : OpprettSykmeldingAktivitet

    data class Behandlingsdager(
        val antallBehandlingsdager: Int,
        override val fom: LocalDate,
        override val tom: LocalDate
    ) : OpprettSykmeldingAktivitet

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: LocalDate,
        override val tom: LocalDate
    ) : OpprettSykmeldingAktivitet

    data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
        OpprettSykmeldingAktivitet
}

data class OpprettSykmeldingMedisinskArsak(val isMedisinskArsak: Boolean)

data class OpprettSykmeldingArbeidsrelatertArsak(
    val isArbeidsrelatertArsak: Boolean,
    val arbeidsrelaterteArsaker: List<SykInnArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?
)

data class OpprettSykmeldingDiagnoseInfo(
    val system: DiagnoseSystem,
    val code: String,
)
