package no.nav.tsm.sykinnapi.modell.syfosmregister

data class SporsmalSvarDTO(
    val sporsmal: String?,
    val svar: String,
    val restriksjoner: List<SvarRestriksjonDTO>,
)
