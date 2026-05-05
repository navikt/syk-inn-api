package no.nav.tsm.modules.sykmeldinger.domain

import java.time.OffsetDateTime
import java.util.*
import no.nav.tsm.core.common.Navn
import no.nav.tsm.sykmelding.input.core.model.RuleType

enum class SykInnSykmeldingType {
    DIGITAL,
    XML,
    PAPIR,
    UTENLANDSK,
}

sealed interface SykInnSykmelding {
    val values: SykInnSykmeldingValues
    val type: SykInnSykmeldingType
}

/**
 * Lite utgangspunkt på det sentrale Domeneobjektet for Syk Inn API, ingenting her er satt i stein.
 */
class VerifiedSykInnSykmelding(
    val sykmeldingId: UUID,
    override val values: SykInnSykmeldingValues,
    val meta: SykInnSykmeldingMeta,
    val result: SykInnSykmeldingRuleResult,
    override val type: SykInnSykmeldingType,
) : SykInnSykmelding

data class UnverifiedSykInnSykmelding(
    val submitId: UUID,
    override val values: SykInnSykmeldingValues,
    val meta: UnverifiedSykInnSykmeldingMeta,
    override val type: SykInnSykmeldingType = SykInnSykmeldingType.DIGITAL,
) : SykInnSykmelding

data class SykInnPasient(
    override val fornavn: String?,
    override val mellomnavn: String?,
    override val etternavn: String?,
    val ident: String,
) : Navn

data class SykInnBehandler(
    override val fornavn: String?,
    override val mellomnavn: String?,
    override val etternavn: String?,
    val ident: String,
    val hpr: String,
    val helsepersonellkategori: List<String>,
) : Navn

sealed interface SykInnSykmeldingMeta {

    val source: String
    val mottatt: OffsetDateTime
    val pasient: SykInnPasient

    data class Legacy(
        override val source: String,
        override val mottatt: OffsetDateTime,
        override val pasient: SykInnPasient,
        val behandler: SykInnBehandler,
        val legekontorOrgnr: String?,
        val legekontorTlf: String?,
    ) : SykInnSykmeldingMeta

    data class Digital(
        override val source: String,
        override val mottatt: OffsetDateTime,
        override val pasient: SykInnPasient,
        val behandler: SykInnBehandler,
        val legekontorOrgnr: String,
        val legekontorTlf: String,
    ) : SykInnSykmeldingMeta

    data class Utenlandsk(
        override val source: String,
        override val mottatt: OffsetDateTime,
        override val pasient: SykInnPasient,
    ) : SykInnSykmeldingMeta
}

sealed interface SykInnSykmeldingRuleResult {

    object OK : SykInnSykmeldingRuleResult

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
