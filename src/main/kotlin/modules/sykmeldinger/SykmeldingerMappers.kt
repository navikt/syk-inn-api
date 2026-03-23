package no.nav.tsm.modules.sykmeldinger

import java.time.OffsetDateTime
import java.time.ZoneOffset
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder

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
