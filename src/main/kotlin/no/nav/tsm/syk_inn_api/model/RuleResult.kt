package no.nav.tsm.syk_inn_api.model

import java.time.OffsetDateTime

enum class TilbakedatertMerknad {
    TILBAKEDATERING_UNDER_BEHANDLING,
    TILBAKEDATERING_UGYLDIG_TILBAKEDATERING,
    TILBAKEDATERING_KREVER_FLERE_OPPLYSNINGER,
    TILBAKEDATERING_DELVIS_GODKJENT,
    TILBAKEDATERING_TILBAKEDATERT_PAPIRSYKMELDING
}

data class ValidationResult(
    val status: RuleType,
    val timestamp: OffsetDateTime,
    val rules: List<Rule>
)

enum class RuleType {
    OK,
    PENDING,
    INVALID
}

enum class RuleResult {
    OK,
    INVALID
}

enum class ValidationType {
    AUTOMATIC,
    MANUAL
}

data class RuleOutcome(val outcome: RuleResult, val timestamp: OffsetDateTime)

sealed interface Rule {
    val type: RuleType
    val timestamp: OffsetDateTime
    val name: String
    val description: String
    val validationType: ValidationType
}

data class InvalidRule(
    override val name: String,
    override val description: String,
    override val timestamp: OffsetDateTime,
    override val validationType: ValidationType,
) : Rule {
    override val type = RuleType.INVALID
    val outcome = RuleOutcome(RuleResult.INVALID, timestamp)
}

data class PendingRule(
    override val name: String,
    override val timestamp: OffsetDateTime,
    override val description: String,
    override val validationType: ValidationType
) : Rule {
    override val type = RuleType.PENDING
}

data class OKRule(
    override val name: String,
    override val description: String,
    override val timestamp: OffsetDateTime,
    override val validationType: ValidationType
) : Rule {
    override val type = RuleType.OK
    val outcome: RuleOutcome = RuleOutcome(RuleResult.OK, timestamp)
}
