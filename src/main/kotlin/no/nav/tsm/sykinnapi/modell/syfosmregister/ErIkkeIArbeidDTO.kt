package no.nav.tsm.sykinnapi.modell.syfosmregister

import java.time.LocalDate

data class ErIkkeIArbeidDTO(
    val arbeidsforPaSikt: Boolean,
    val arbeidsforFOM: LocalDate?,
    val vurderingsdato: LocalDate?,
)
