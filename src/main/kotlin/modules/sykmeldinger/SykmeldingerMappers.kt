package modules.sykmeldinger

import java.time.OffsetDateTime
import java.time.ZoneOffset
import modules.sykmeldinger.domain.SykInnSykmeldingMeta
import modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import modules.sykmeldinger.sykmelder.Sykmelder

fun UnverifiedSykInnSykmelding.toVerifiedSykmelding(
    rules: SykInnSykmeldingRuleResult,
    sykmelder: Sykmelder,
): VerifiedSykInnSykmelding {
    return VerifiedSykInnSykmelding(
        sykmeldingId = sykmeldingId,
        values = values,
        meta =
            SykInnSykmeldingMeta(
                // TODO: Ikke .now()?
                mottatt = OffsetDateTime.now(ZoneOffset.UTC),
                pasientIdent = meta.pasientIdent,
                hpr = sykmelder.hpr,
                legekontorOrgnr = meta.legekontorOrgnr,
                legekontorTlf = meta.legekontorTlf,
            ),
        result = rules,
    )
}
