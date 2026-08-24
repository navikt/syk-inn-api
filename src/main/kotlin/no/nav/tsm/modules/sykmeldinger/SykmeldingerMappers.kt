package no.nav.tsm.modules.sykmeldinger

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import no.nav.tsm.modules.sykmeldinger.domain.*
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder
import no.nav.tsm.pdl.Person

fun UnverifiedSykInnSykmelding.toVerifiedSykmelding(
    rules: SykInnSykmeldingRuleResult,
    sykmelder: Sykmelder.MedSuspensjon,
    pasient: Person,
): VerifiedSykInnSykmelding {
    val verifiedAt = OffsetDateTime.now(ZoneOffset.UTC)

    return VerifiedSykInnSykmelding(
        sykmeldingId = UUID.randomUUID(),
        values = values,
        meta =
            SykInnSykmeldingMeta.Digital(
                source = meta.source,
                mottatt = verifiedAt,
                pasient =
                    SykInnPasient(
                        ident = meta.pasientIdent,
                        fornavn = pasient.navn?.fornavn,
                        mellomnavn = pasient.navn?.mellomnavn,
                        etternavn = pasient.navn?.etternavn,
                    ),
                behandler =
                    SykInnBehandler(
                        fornavn = sykmelder.navn.fornavn,
                        mellomnavn = sykmelder.navn.mellomnavn,
                        etternavn = sykmelder.navn.etternavn,
                        hpr = sykmelder.hpr,
                        ident = sykmelder.ident,
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
        type = SykInnSykmeldingType.DIGITAL,
        result = rules,
    )
}
