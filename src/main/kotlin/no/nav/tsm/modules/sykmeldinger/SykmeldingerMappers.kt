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
    val verifiedAt = OffsetDateTime.now(ZoneOffset.UTC)

    return VerifiedSykInnSykmelding(
        sykmeldingId = UUID.randomUUID(),
        values = values,
        meta =
            SykInnSykmeldingMeta(
                source = meta.source,
                mottatt = verifiedAt,
                pasient =
                    SykInnPasient(
                        ident = meta.pasientIdent,
                        fornavn = pasient.navn.fornavn,
                        mellomnavn = pasient.navn.mellomnavn,
                        etternavn = pasient.navn.etternavn,
                    ),
                behandler =
                    SykInnBehandler(
                        fornavn = sykmelder.navn.fornavn,
                        mellomnavn = sykmelder.navn.mellomnavn,
                        etternavn = sykmelder.navn.etternavn,
                        hpr = sykmelder.hpr,
                        helsepersonellkategori =
                            sykmelder.godkjenninger
                                .filter { it.helsepersonellkategori?.aktiv == true }
                                .mapNotNull { it.helsepersonellkategori?.verdi }
                                .ifEmpty {
                                    throw IllegalStateException(
                                        "Behandler without aktiv helsepersonellkategori ${sykmelder.hpr}"
                                    )
                                },
                    ),
                legekontorOrgnr = meta.legekontorOrgnr,
                legekontorTlf = meta.legekontorTlf,
            ),
        result = rules,
    )
}
