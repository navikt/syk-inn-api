package no.nav.tsm.modules.sykmeldinger

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder

fun UnverifiedSykInnSykmelding.toVerifiedSykmelding(
    rules: SykInnSykmeldingRuleResult,
    sykmelder: Sykmelder.MedSuspensjon,
): VerifiedSykInnSykmelding {
    return VerifiedSykInnSykmelding(
        sykmeldingId = UUID.randomUUID(),
        values = values,
        meta =
            SykInnSykmeldingMeta(
                // TODO
                source = "TODO",
                // TODO: Ikke .now()?
                mottatt = OffsetDateTime.now(ZoneOffset.UTC),
                pasientIdent = meta.pasientIdent,
                behandlerHpr = sykmelder.hpr,
                behandlerNavn = sykmelder.navn,
                legekontorOrgnr = meta.legekontorOrgnr,
                legekontorTlf = meta.legekontorTlf,
            ),
        result = rules,
    )
}
