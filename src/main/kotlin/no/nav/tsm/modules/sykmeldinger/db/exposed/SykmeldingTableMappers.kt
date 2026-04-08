package no.nav.tsm.modules.sykmeldinger.db.exposed

import no.nav.tsm.modules.sykmeldinger.domain.SykInnDiagnoseInfo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.text
import no.nav.tsm.sykmelding.input.core.model.RuleType

fun SykInnSykmeldingRuleResult.toRuleResultColumn(): SykmeldingJsonbRuleResult =
    when (this) {
        is SykInnSykmeldingRuleResult.OK -> SykmeldingJsonbRuleResult(RuleType.OK, null, null)
        is SykInnSykmeldingRuleResult.Outcome ->
            SykmeldingJsonbRuleResult(type = this.type, message = this.message, rule = this.rule)
    }

fun SykInnDiagnoseInfo?.toDiagnoseJsonb(): SykmeldingJsonbDiagnose? =
    this?.let { SykmeldingJsonbDiagnose(system = it.system.name, text = it.text(), code = it.code) }
