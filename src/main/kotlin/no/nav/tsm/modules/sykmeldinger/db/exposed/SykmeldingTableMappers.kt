package no.nav.tsm.modules.sykmeldinger.db.exposed

import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.sykmelding.input.core.model.RuleType

fun SykInnSykmeldingRuleResult.toRuleResultColumn(): SykmeldingColumnRuleResult =
    when (this) {
        is SykInnSykmeldingRuleResult.OK -> SykmeldingColumnRuleResult(RuleType.OK, null, null)
        is SykInnSykmeldingRuleResult.Outcome ->
            SykmeldingColumnRuleResult(type = this.type, message = this.message, rule = this.rule)
    }
