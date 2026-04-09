package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import java.time.OffsetDateTime
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.sykmelding.input.core.model.DigitalSykmelding
import no.nav.tsm.sykmelding.input.core.model.InvalidRule
import no.nav.tsm.sykmelding.input.core.model.PendingRule
import no.nav.tsm.sykmelding.input.core.model.Reason
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.ValidationType
import no.nav.tsm.sykmelding.input.core.model.metadata.Digital

fun VerifiedSykInnSykmelding.toInputRecord(): SykmeldingRecord {
    return SykmeldingRecord(
        metadata = Digital(orgnummer = meta.legekontorOrgnr),
        validation = result.toValidationResult(meta.mottatt),
        sykmelding =
            DigitalSykmelding(
                id = sykmeldingId.toString(),
                metadata = TODO(),
                pasient = TODO(),
                medisinskVurdering = TODO(),
                aktivitet = TODO(),
                behandler = TODO(),
                sykmelder = TODO(),
                arbeidsgiver = TODO(),
                tilbakedatering = TODO(),
                bistandNav = TODO(),
                utdypendeSporsmal = TODO(),
            ),
    )
}

private fun SykInnSykmeldingRuleResult.toValidationResult(
    mottatt: OffsetDateTime
): ValidationResult =
    when (this) {
        is SykInnSykmeldingRuleResult.OK -> ValidationResult(RuleType.OK, mottatt, emptyList())
        is SykInnSykmeldingRuleResult.Outcome -> {
            val rule =
                when (type) {
                    RuleType.OK -> throw IllegalStateException("Rule with outcome can't be OK")
                    RuleType.PENDING ->
                        PendingRule(rule, mottatt, ValidationType.AUTOMATIC, Reason(TODO(), TODO()))

                    RuleType.INVALID ->
                        InvalidRule(rule, ValidationType.AUTOMATIC, mottatt, Reason(TODO(), TODO()))
                }

            ValidationResult(type, mottatt, listOf(rule))
        }
    }
