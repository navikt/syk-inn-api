package no.nav.tsm.sykinnapi.modell.syfosmregister

import java.time.LocalDate

data class KontaktMedPasientDTO(
    val kontaktDato: LocalDate?,
    val begrunnelseIkkeKontakt: String?,
)
