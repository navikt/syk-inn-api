package no.nav.tsm.sykinnapi.modell.syfosmregister

import java.time.LocalDate

data class SykmeldingsperiodeDTO(
    val fom: LocalDate,
    val tom: LocalDate,
    val gradert: GradertDTO?,
    val behandlingsdager: Int?,
    val innspillTilArbeidsgiver: String?,
    val type: PeriodetypeDTO,
    val aktivitetIkkeMulig: AktivitetIkkeMuligDTO?,
    val reisetilskudd: Boolean,
)
