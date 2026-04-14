package no.nav.tsm.modules.sykmeldinger.db.sykmelding

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.OptBoolean
import java.time.LocalDate
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.RuleType

data class SykmeldingJsonbNavn(val fornavn: String, val mellomnavn: String?, val etternavn: String)

data class SykmeldingJsonbRuleResult(val type: RuleType, val message: String?, val rule: String?)

data class SykmeldingJsonbDiagnose(val system: String, val text: String, val code: String)

data class SykmeldingJsonbMeldinger(val tilNav: String?, val tilArbeidsgiver: String?)

data class SykmeldingJsonbYrkesskade(val yrkesskade: Boolean, val skadedato: LocalDate?)

data class SykmeldingJsonbArbeidsgiver(val harFlere: Boolean, val arbeidsgivernavn: String)

data class SykmeldingJsonbTilbakedatering(val startdato: LocalDate, val begrunnelse: String)

data class SykmeldingJsonbUtdypendeSporsmal(val sporsmalstekst: String, val svar: String)

@JsonSubTypes(
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.Behandlingsdager::class, name = "BEHANDLINGSDAGER"),
    JsonSubTypes.Type(SykmeldingJsonbAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "type",
    requireTypeIdForSubtypes = OptBoolean.TRUE,
)
sealed interface SykmeldingJsonbAktivitet {
    val fom: LocalDate
    val tom: LocalDate

    data class IkkeMulig(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val arbeidsrelatertArsak: SykmeldingJsonbArbeidsrelatertArsak,
    ) : SykmeldingJsonbAktivitet

    data class Gradert(
        val grad: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
        val reisetilskudd: Boolean,
    ) : SykmeldingJsonbAktivitet

    data class Behandlingsdager(
        val antallBehandlingsdager: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : SykmeldingJsonbAktivitet

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : SykmeldingJsonbAktivitet

    data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
        SykmeldingJsonbAktivitet
}

data class SykmeldingJsonbMedisinskArsak(val isMedisinskArsak: Boolean)

data class SykmeldingJsonbArbeidsrelatertArsak(
    val isArbeidsrelatertArsak: Boolean,
    val arbeidsrelaterteArsaker: List<ArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?,
)
