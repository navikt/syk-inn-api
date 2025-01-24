package no.nav.tsm.sykinnapi.modell.syfosmregister

data class RegelinfoDTO(
    val messageForSender: String,
    val messageForUser: String,
    val ruleName: String,
    val ruleStatus: RegelStatusDTO?,
)
