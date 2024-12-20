package no.nav.tsm.sykinnapi.modell.receivedSykmelding

data class ValidationResult(val status: Status, val ruleHits: List<RuleInfo> = emptyList()) {
    companion object {
        val OK = ValidationResult(Status.OK)
    }
}

data class RuleInfo(
    val ruleName: String,
    val messageForSender: String,
    val messageForUser: String,
    val ruleStatus: Status
)

enum class Status {
    OK,
    MANUAL_PROCESSING,
    INVALID
}
