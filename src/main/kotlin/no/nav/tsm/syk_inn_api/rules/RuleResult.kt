package no.nav.tsm.syk_inn_api.rules

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

data class Reason(
    val sykmeldt: String,
    val sykmelder: String,
)

enum class RuleType {
    OK,
    PENDING,
    INVALID
}

enum class ValidationType {
    AUTOMATIC,
    MANUAL
}

sealed interface Rule {
    val type: RuleType
    val name: String
    val validationType: ValidationType
    val timestamp: OffsetDateTime
}

data class InvalidRule(
    override val name: String,
    override val validationType: ValidationType,
    override val timestamp: OffsetDateTime,
    val reason: Reason
) : Rule {
    override val type = RuleType.INVALID
}

data class PendingRule(
    override val name: String,
    override val timestamp: OffsetDateTime,
    override val validationType: ValidationType,
    val reason: Reason,
) : Rule {
    override val type = RuleType.PENDING
}

data class OKRule(
    override val name: String,
    override val timestamp: OffsetDateTime,
    override val validationType: ValidationType
) : Rule {
    override val type = RuleType.OK
}
