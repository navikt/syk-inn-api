package no.nav.tsm.modules.behandler.access

import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingAktivitet
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingArbeidsgiver
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingArbeidsrelatertArsak
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingDiagnoseInfo
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingFull
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingMedisinskArsak
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingMeldinger
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingMeta
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingRedacted
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingRedactedAktivitet
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingRedactedValues
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingRuleResult
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingSporsmalSvar
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingSykmelder
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingSykmeldt
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingTilbakedatering
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingUtdypendeSporsmal
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingValues
import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingYrkesskade
import no.nav.tsm.modules.sykmeldinger.domain.SykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.domain.SykInnArbeidsgiver
import no.nav.tsm.modules.sykmeldinger.domain.SykInnDiagnoseInfo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnMeldinger
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingValues
import no.nav.tsm.modules.sykmeldinger.domain.SykInnTilbakedatering
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmal
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmalSvar
import no.nav.tsm.modules.sykmeldinger.domain.SykInnYrkesskade
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.text
import no.nav.tsm.sykmelding.input.core.model.RuleType

fun VerifiedSykInnSykmelding.toSykmelding() =
    BehandlerSykmeldingFull(
        sykmeldingId = sykmeldingId,
        meta = meta.toBehandlerSykmeldingMeta(),
        values = values.toSykmeldingDocumentValues(),
        utfall = result.resultToUtfall(),
    )

fun VerifiedSykInnSykmelding.toRedactedSykmelding(): BehandlerSykmeldingRedacted =
    BehandlerSykmeldingRedacted(
        sykmeldingId = sykmeldingId,
        meta = meta.toBehandlerSykmeldingMeta(),
        utfall = BehandlerSykmeldingRuleResult(result = RuleType.OK, cause = null),
        values =
            BehandlerSykmeldingRedactedValues(
                aktivitet =
                    values.aktivitet.map { aktivitet ->
                        BehandlerSykmeldingRedactedAktivitet(
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                            type =
                                when (aktivitet) {
                                    is SykInnAktivitet.IkkeMulig -> "AKTIVITET_IKKE_MULIG"
                                    is SykInnAktivitet.Gradert -> "GRADERT"
                                    is SykInnAktivitet.Behandlingsdager -> "BEHANDLINGSDAGER"
                                    is SykInnAktivitet.Avventende -> "AVVENTENDE"
                                    is SykInnAktivitet.Reisetilskudd -> "REISETILSKUDD"
                                },
                        )
                    }
            ),
    )

private fun SykInnSykmeldingRuleResult.resultToUtfall() =
    when (this) {
        is SykInnSykmeldingRuleResult.OK ->
            BehandlerSykmeldingRuleResult(result = RuleType.OK, cause = null)
        is SykInnSykmeldingRuleResult.Outcome ->
            BehandlerSykmeldingRuleResult(result = this.type, cause = this.message)
    }

private fun SykInnSykmeldingMeta.toBehandlerSykmeldingMeta() =
    BehandlerSykmeldingMeta(
        mottatt = mottatt,
        pasient = BehandlerSykmeldingSykmeldt(ident = pasient.ident, navn = pasient.displayName()),
        sykmelder =
            BehandlerSykmeldingSykmelder(hpr = behandler.hpr, navn = behandler.displayName()),
        legekontorOrgnr = legekontorOrgnr,
        legekontorTlf = legekontorTlf,
    )

fun SykInnSykmeldingValues.toSykmeldingDocumentValues(): BehandlerSykmeldingValues {
    return BehandlerSykmeldingValues(
        hoveddiagnose = this.hoveddiagnose?.toExistingSykmeldingDiagnoseInfo(),
        bidiagnoser = this.bidiagnoser.toExistingSykmeldingDiagnoseInfo(),
        aktivitet = this.aktivitet.toPersistedSykmeldingAktivitetToExistingSykmeldingAktivitet(),
        svangerskapsrelatert = this.svangerskapsrelatert,
        pasientenSkalSkjermes = this.pasientenSkalSkjermes,
        meldinger = this.meldinger?.toExistingSykmeldingMeldinger(),
        yrkesskade = this.yrkesskade?.toExistingSykmeldingYrkesskade(),
        arbeidsgiver = this.arbeidsgiver?.toExistingSykmeldingArbeidsgiver(),
        tilbakedatering = this.tilbakedatering?.toExistingSykmeldingTilbakedatering(),
        utdypendeSporsmal = this.utdypendeSporsmal?.toExistingSykmeldingUtdypendeSporsmalSvar(),
        annenFravarsgrunn = this.annenFravarsgrunn,
    )
}

private fun List<SykInnAktivitet>.toPersistedSykmeldingAktivitetToExistingSykmeldingAktivitet():
    List<BehandlerSykmeldingAktivitet> =
    this.map { aktivitet ->
        when (aktivitet) {
            is SykInnAktivitet.IkkeMulig ->
                BehandlerSykmeldingAktivitet.IkkeMulig(
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                    medisinskArsak =
                        BehandlerSykmeldingMedisinskArsak(
                            isMedisinskArsak = aktivitet.medisinskArsak.isMedisinskArsak
                        ),
                    arbeidsrelatertArsak =
                        BehandlerSykmeldingArbeidsrelatertArsak(
                            isArbeidsrelatertArsak =
                                aktivitet.arbeidsrelatertArsak.isArbeidsrelatertArsak,
                            arbeidsrelaterteArsaker =
                                aktivitet.arbeidsrelatertArsak.arbeidsrelaterteArsaker,
                            annenArbeidsrelatertArsak =
                                aktivitet.arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                        ),
                )

            is SykInnAktivitet.Gradert ->
                BehandlerSykmeldingAktivitet.Gradert(
                    grad = aktivitet.grad,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                    reisetilskudd = aktivitet.reisetilskudd,
                )

            is SykInnAktivitet.Behandlingsdager ->
                BehandlerSykmeldingAktivitet.Behandlingsdager(
                    antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                )

            is SykInnAktivitet.Avventende ->
                BehandlerSykmeldingAktivitet.Avventende(
                    innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver,
                    fom = aktivitet.fom,
                    tom = aktivitet.tom,
                )

            is SykInnAktivitet.Reisetilskudd ->
                BehandlerSykmeldingAktivitet.Reisetilskudd(fom = aktivitet.fom, tom = aktivitet.tom)
        }
    }

private fun List<SykInnDiagnoseInfo>.toExistingSykmeldingDiagnoseInfo():
    List<BehandlerSykmeldingDiagnoseInfo>? {
    if (this.isEmpty()) return null

    return this.map { diagnose ->
        BehandlerSykmeldingDiagnoseInfo(
            system = diagnose.system,
            code = diagnose.code,
            text = diagnose.text(),
        )
    }
}

private fun SykInnTilbakedatering.toExistingSykmeldingTilbakedatering():
    BehandlerSykmeldingTilbakedatering =
    BehandlerSykmeldingTilbakedatering(startdato = startdato, begrunnelse = begrunnelse)

private fun SykInnUtdypendeSporsmal.toExistingSykmeldingUtdypendeSporsmalSvar():
    BehandlerSykmeldingUtdypendeSporsmal =
    BehandlerSykmeldingUtdypendeSporsmal(
        hensynPaArbeidsplassen = hensynPaArbeidsplassen?.toExistingSykmeldingSporsmalSvar(),
        medisinskOppsummering = medisinskOppsummering?.toExistingSykmeldingSporsmalSvar(),
        utfordringerMedArbeid = utfordringerMedArbeid?.toExistingSykmeldingSporsmalSvar(),
        sykdomsutvikling = sykdomsutvikling?.toExistingSykmeldingSporsmalSvar(),
        arbeidsrelaterteUtfordringer =
            arbeidsrelaterteUtfordringer?.toExistingSykmeldingSporsmalSvar(),
        behandlingOgFremtidigArbeid =
            behandlingOgFremtidigArbeid?.toExistingSykmeldingSporsmalSvar(),
        uavklarteForhold = uavklarteForhold?.toExistingSykmeldingSporsmalSvar(),
        oppdatertMedisinskStatus = oppdatertMedisinskStatus?.toExistingSykmeldingSporsmalSvar(),
        realistiskMestringArbeid = realistiskMestringArbeid?.toExistingSykmeldingSporsmalSvar(),
        forventetHelsetilstandUtvikling =
            forventetHelsetilstandUtvikling?.toExistingSykmeldingSporsmalSvar(),
        medisinskeHensyn = medisinskeHensyn?.toExistingSykmeldingSporsmalSvar(),
    )

private fun SykInnUtdypendeSporsmalSvar.toExistingSykmeldingSporsmalSvar():
    BehandlerSykmeldingSporsmalSvar =
    BehandlerSykmeldingSporsmalSvar(sporsmalstekst = sporsmalstekst, svar = svar)

private fun SykInnArbeidsgiver.toExistingSykmeldingArbeidsgiver(): BehandlerSykmeldingArbeidsgiver =
    BehandlerSykmeldingArbeidsgiver(harFlere = harFlere, arbeidsgivernavn = arbeidsgivernavn)

private fun SykInnYrkesskade.toExistingSykmeldingYrkesskade(): BehandlerSykmeldingYrkesskade =
    BehandlerSykmeldingYrkesskade(yrkesskade = yrkesskade, skadedato = skadedato)

private fun SykInnMeldinger.toExistingSykmeldingMeldinger(): BehandlerSykmeldingMeldinger =
    BehandlerSykmeldingMeldinger(tilNav = this.tilNav, tilArbeidsgiver = this.tilArbeidsgiver)

private fun SykInnDiagnoseInfo.toExistingSykmeldingDiagnoseInfo(): BehandlerSykmeldingDiagnoseInfo =
    BehandlerSykmeldingDiagnoseInfo(system = system, code = code, text = this.text())
