package no.nav.tsm.modules.sykmeldinger.domain

import java.time.OffsetDateTime
import java.util.*
import no.nav.tsm.core.common.name.Navn
import no.nav.tsm.sykmelding.input.core.model.RuleType

sealed interface SykInnSykmelding {
    val values: SykInnSykmeldingValues
}

/**
 * Lite utgangspunkt på det sentrale Domeneobjektet for Syk Inn API, ingenting her er satt i stein.
 */
class VerifiedSykInnSykmelding(
    val sykmeldingId: UUID,
    override val values: SykInnSykmeldingValues,
    val meta: SykInnSykmeldingMeta,
    val result: SykInnSykmeldingRuleResult,
) : SykInnSykmelding

data class UnverifiedSykInnSykmelding(
    val submitId: UUID,
    override val values: SykInnSykmeldingValues,
    val meta: UnverifiedSykInnSykmeldingMeta,
) : SykInnSykmelding

data class SykInnPasient(
    override val fornavn: String,
    override val mellomnavn: String?,
    override val etternavn: String,
    val ident: String,
) : Navn

data class SykInnBehandler(
    override val fornavn: String,
    override val mellomnavn: String?,
    override val etternavn: String,
    val hpr: String,
) : Navn

data class SykInnSykmeldingMeta(
    val source: String,
    val mottatt: OffsetDateTime,
    val pasient: SykInnPasient,
    val behandler: SykInnBehandler,
    val legekontorOrgnr: String,
    val legekontorTlf: String,
)

sealed interface SykInnSykmeldingRuleResult {

    class OK() : SykInnSykmeldingRuleResult

    data class Outcome(val type: RuleType, val message: String, val rule: String) :
        SykInnSykmeldingRuleResult {
        init {
            require(type != RuleType.OK) {
                "Outcome cannot be OK, only PENDING and INVALID should have message"
            }
        }
    }
}

data class UnverifiedSykInnSykmeldingMeta(
    val source: String,
    val behandlerHpr: String,
    val pasientIdent: String,
    val legekontorOrgnr: String,
    val legekontorTlf: String,
)
