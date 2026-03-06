package modules.sykmeldinger.domain

import java.time.OffsetDateTime
import java.util.UUID
import no.nav.tsm.sykmelding.input.core.model.RuleType

sealed interface SykInnSykmelding {
    val sykmeldingId: UUID
    val values: SykInnSykmeldingValues
}

/**
 * Lite utgangspunkt på det sentrale Domeneobjektet for Syk Inn API, ingenting her er satt i stein.
 */
data class RuledSykInnSykmelding(
    override val sykmeldingId: UUID,
    override val values: SykInnSykmeldingValues,
    val meta: SykInnSykmeldingMeta,
    val result: SykInnSykmeldingRuleResult,
) : SykInnSykmelding

data class SykInnSykmeldingMeta(val mottatt: OffsetDateTime)

sealed interface SykInnSykmeldingRuleResult {
    class OK : SykInnSykmeldingRuleResult

    data class Outcome(val type: RuleType, val message: String) : SykInnSykmeldingRuleResult {
        init {
            require(type != RuleType.OK) {
                "Outcome cannot be OK, only PENDING and INVALID should have message"
            }
        }
    }
}

data class UnruledSykInnSykmelding(
    override val sykmeldingId: UUID,
    override val values: SykInnSykmeldingValues,
    val meta: UnruledSykInnSykmledingMeta,
) : SykInnSykmelding

data class UnruledSykInnSykmledingMeta(
    val behandlerHpr: String,
    val pasientIdent: String,
    val legekontorOrgnr: String,
)
