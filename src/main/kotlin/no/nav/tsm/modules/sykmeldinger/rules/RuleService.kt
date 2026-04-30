package no.nav.tsm.modules.sykmeldinger.rules

import arrow.core.Either
import arrow.core.left
import arrow.core.right
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDateTime
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.pdl.PdlPerson
import no.nav.tsm.modules.sykmeldinger.rules.mappers.mapPdlPersonToRegulaPasient
import no.nav.tsm.modules.sykmeldinger.rules.mappers.mapSykmelderToRegulaBehandler
import no.nav.tsm.modules.sykmeldinger.rules.mappers.mapUnruledSykInnSykmeldingToRegulaPayload
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder
import no.nav.tsm.regulus.regula.*
import no.nav.tsm.regulus.regula.executor.ExecutionMode
import no.nav.tsm.sykmelding.input.core.model.RuleType

enum class RuleErrors {
    InvalidPatient
}

class RuleService {
    private val logger = logger()

    @WithSpan
    fun verify(
        sykmelding: UnverifiedSykInnSykmelding,
        otherSykmeldinger: List<VerifiedSykInnSykmelding>,
        sykmelder: Sykmelder,
        sykmeldt: PdlPerson,
    ): Either<RuleErrors, Pair<RegulaResult, List<RegulaJuridiskVurdering>>> {
        val now = LocalDateTime.now()

        val regulaPasient = sykmeldt.mapPdlPersonToRegulaPasient()
        if (regulaPasient == null) {
            logger.error(
                "Unable to execute rules for pasient with missing or invalid ident or fødselsdato in PDL"
            )

            return RuleErrors.InvalidPatient.left()
        }

        val regulaBehandler =
            sykmelder.mapSykmelderToRegulaBehandler(sykmelding.meta.legekontorOrgnr)

        return this.executeRegulaRules(
                behandletTidspunkt = now,
                sykmelding = sykmelding,
                otherSykmeldinger = otherSykmeldinger,
                behandler = regulaBehandler,
                pasient = regulaPasient,
            )
            .right()
    }

    @WithSpan
    private fun executeRegulaRules(
        behandletTidspunkt: LocalDateTime,
        sykmelding: UnverifiedSykInnSykmelding,
        otherSykmeldinger: List<VerifiedSykInnSykmelding>,
        behandler: RegulaBehandler,
        pasient: RegulaPasient,
    ): Pair<RegulaResult, List<RegulaJuridiskVurdering>> {
        val regulaExecutionPayload =
            mapUnruledSykInnSykmeldingToRegulaPayload(
                behandletTidspunkt = behandletTidspunkt,
                sykmelding = sykmelding,
                otherSykmeldinger = otherSykmeldinger,
                behandler = behandler,
                pasient = pasient,
            )

        val result =
            executeRegulaRules(
                ruleExecutionPayload = regulaExecutionPayload,
                mode = ExecutionMode.NORMAL,
            )

        return result to result.juridisk
    }
}

fun RegulaResult.toSykInnRuleResult(): SykInnSykmeldingRuleResult =
    when (this) {
        is RegulaResult.Ok -> SykInnSykmeldingRuleResult.OK
        is RegulaResult.NotOk ->
            SykInnSykmeldingRuleResult.Outcome(
                message = this.outcome.reason.sykmelder,
                rule = this.outcome.rule,
                type =
                    when (this.status) {
                        RegulaStatus.MANUAL_PROCESSING -> RuleType.PENDING
                        RegulaStatus.INVALID -> RuleType.INVALID
                        RegulaStatus.OK ->
                            throw IllegalStateException("RegulaResult.NotOk cannot have status OK")
                    },
            )
    }
