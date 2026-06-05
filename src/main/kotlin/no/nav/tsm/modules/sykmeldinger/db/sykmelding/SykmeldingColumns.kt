package no.nav.tsm.modules.sykmeldinger.db.sykmelding

import java.time.LocalDate
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import no.nav.tsm.sykmelding.input.core.model.RuleType

data class SykmeldingJsonbNavn(
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

data class SykmeldingJsonbValidationResult(
    val type: RuleType,
    val message: String?,
    val rule: String?,
)

data class SykmeldingJsonbDiagnose(val system: String, val text: String?, val code: String)

data class SykmeldingJsonbMeldinger(val tilNav: String?, val tilArbeidsgiver: String?)

data class SykmeldingJsonbYrkesskade(val yrkesskade: Boolean, val skadedato: LocalDate?)

data class SykmeldingJsonbArbeidsgiver(val harFlere: Boolean, val arbeidsgivernavn: String?)

data class SykmeldingJsonbTilbakedatering(val startdato: LocalDate?, val begrunnelse: String?)

data class SykmeldingJsonbUtdypendeSporsmal(val sporsmalstekst: String, val svar: String)

enum class SykmeldingJsonbAktivitetType {
    AKTIVITET_IKKE_MULIG,
    GRADERT,
    AVVENTENDE,
    BEHANDLINGSDAGER,
    REISETILSKUDD,
}

sealed interface SykmeldingJsonbAktivitet {
    val type: SykmeldingJsonbAktivitetType
    val fom: LocalDate
    val tom: LocalDate

    data class IkkeMulig(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val arbeidsrelatertArsak: SykmeldingJsonbArbeidsrelatertArsak?,
    ) : SykmeldingJsonbAktivitet {
        override val type: SykmeldingJsonbAktivitetType =
            SykmeldingJsonbAktivitetType.AKTIVITET_IKKE_MULIG
    }

    data class Gradert(
        val grad: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
        val reisetilskudd: Boolean,
    ) : SykmeldingJsonbAktivitet {
        override val type: SykmeldingJsonbAktivitetType = SykmeldingJsonbAktivitetType.GRADERT
    }

    data class Behandlingsdager(
        val antallBehandlingsdager: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : SykmeldingJsonbAktivitet {
        override val type: SykmeldingJsonbAktivitetType =
            SykmeldingJsonbAktivitetType.BEHANDLINGSDAGER
    }

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : SykmeldingJsonbAktivitet {
        override val type: SykmeldingJsonbAktivitetType = SykmeldingJsonbAktivitetType.AVVENTENDE
    }

    data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
        SykmeldingJsonbAktivitet {
        override val type: SykmeldingJsonbAktivitetType = SykmeldingJsonbAktivitetType.REISETILSKUDD
    }
}

data class SykmeldingJsonbArbeidsrelatertArsak(
    val arbeidsrelaterteArsaker: List<ArbeidsrelatertArsakType>,
    val annenArbeidsrelatertArsak: String?,
)
