package no.nav.tsm.sykinnapi.modell.syfosmregister

data class BehandlingsutfallDTO(
    val status: RegelStatusDTO,
    val ruleHits: List<RegelinfoDTO>,
)
