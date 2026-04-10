package no.nav.tsm.modules.sykmeldinger

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import no.nav.tsm.modules.sykmeldinger.domain.SykInnBehandler
import no.nav.tsm.modules.sykmeldinger.domain.SykInnPasient
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.pdl.PdlPerson
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder

fun UnverifiedSykInnSykmelding.toVerifiedSykmelding(
    rules: SykInnSykmeldingRuleResult,
    sykmelder: Sykmelder.MedSuspensjon,
    pasient: PdlPerson,
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
                pasient =
                    SykInnPasient(
                        ident = meta.pasientIdent,
                        fornavn = pasient.navn.fornavn,
                        mellomnavn = pasient.navn.mellomnavn,
                        etternavn = pasient.navn.etternavn,
                    ),
                behandler =
                    SykInnBehandler(
                        fornavn = pasient.navn.fornavn,
                        mellomnavn = pasient.navn.mellomnavn,
                        etternavn = pasient.navn.etternavn,
                        hpr = meta.behandlerHpr,
                    ),
                legekontorOrgnr = meta.legekontorOrgnr,
                legekontorTlf = meta.legekontorTlf,
            ),
        result = rules,
    )
}
