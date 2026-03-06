package modules.sykmeldinger.domain

import java.time.LocalDate

sealed interface SykInnAktivitet {

    val fom: LocalDate
    val tom: LocalDate

    data class IkkeMulig(
        override val fom: LocalDate,
        override val tom: LocalDate,
        val medisinskArsak: SykInnMedisinskArsak,
        val arbeidsrelatertArsak: SykInnArbeidsrelatertArsak,
    ) : SykInnAktivitet

    data class Gradert(
        val grad: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
        val reisetilskudd: Boolean,
    ) : SykInnAktivitet

    data class Behandlingsdager(
        val antallBehandlingsdager: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : SykInnAktivitet

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: LocalDate,
        override val tom: LocalDate,
    ) : SykInnAktivitet

    data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
        SykInnAktivitet
}
