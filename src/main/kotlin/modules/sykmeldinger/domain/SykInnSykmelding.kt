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
data class VerifiedSykInnSykmelding(
    override val sykmeldingId: UUID,
    override val values: SykInnSykmeldingValues,
    val meta: SykInnSykmeldingMeta,
    val result: SykInnSykmeldingRuleResult,
) : SykInnSykmelding

data class UnverifiedSykInnSykmelding(
    override val sykmeldingId: UUID,
    override val values: SykInnSykmeldingValues,
    val meta: UnverifiedSykInnSykmeldingMeta,
) : SykInnSykmelding

data class SykInnSykmeldingMeta(
    val mottatt: OffsetDateTime,
    val pasientIdent: String,
    val hpr: String,
    val legekontorOrgnr: String,
    val legekontorTlf: String,
)

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

data class UnverifiedSykInnSykmeldingMeta(
    val behandlerHpr: String,
    val pasientIdent: String,
    val legekontorOrgnr: String,
    val legekontorTlf: String,
)
