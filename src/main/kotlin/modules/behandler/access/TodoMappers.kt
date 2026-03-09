package modules.behandler.access

import modules.behandler.payloads.BehandlerSykmeldingAktivitet
import modules.behandler.payloads.BehandlerSykmeldingArbeidsgiver
import modules.behandler.payloads.BehandlerSykmeldingArbeidsrelatertArsak
import modules.behandler.payloads.BehandlerSykmeldingDiagnoseInfo
import modules.behandler.payloads.BehandlerSykmeldingFull
import modules.behandler.payloads.BehandlerSykmeldingMedisinskArsak
import modules.behandler.payloads.BehandlerSykmeldingMeldinger
import modules.behandler.payloads.BehandlerSykmeldingMeta
import modules.behandler.payloads.BehandlerSykmeldingRedacted
import modules.behandler.payloads.BehandlerSykmeldingRedactedAktivitet
import modules.behandler.payloads.BehandlerSykmeldingRedactedValues
import modules.behandler.payloads.BehandlerSykmeldingRuleResult
import modules.behandler.payloads.BehandlerSykmeldingSporsmalSvar
import modules.behandler.payloads.BehandlerSykmeldingSykmelder
import modules.behandler.payloads.BehandlerSykmeldingTilbakedatering
import modules.behandler.payloads.BehandlerSykmeldingUtdypendeSporsmal
import modules.behandler.payloads.BehandlerSykmeldingValues
import modules.behandler.payloads.BehandlerSykmeldingYrkesskade
import modules.sykmeldinger.domain.SykInnAktivitet
import modules.sykmeldinger.domain.SykInnArbeidsgiver
import modules.sykmeldinger.domain.SykInnDiagnoseInfo
import modules.sykmeldinger.domain.SykInnMeldinger
import modules.sykmeldinger.domain.SykInnSykmeldingMeta
import modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import modules.sykmeldinger.domain.SykInnSykmeldingValues
import modules.sykmeldinger.domain.SykInnTilbakedatering
import modules.sykmeldinger.domain.SykInnUtdypendeSporsmal
import modules.sykmeldinger.domain.SykInnUtdypendeSporsmalSvar
import modules.sykmeldinger.domain.SykInnYrkesskade
import modules.sykmeldinger.domain.VerifiedSykInnSykmelding
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
            BehandlerSykmeldingRuleResult(result = RuleType.OK, melding = null)
        is SykInnSykmeldingRuleResult.Outcome ->
            BehandlerSykmeldingRuleResult(result = this.type, melding = this.message)
    }

private fun SykInnSykmeldingMeta.toBehandlerSykmeldingMeta() =
    BehandlerSykmeldingMeta(
        mottatt = mottatt,
        pasientIdent = pasientIdent,
        sykmelder = BehandlerSykmeldingSykmelder(hpr = hpr, navn = "TODO: WHAT"),
        legekontorOrgnr = legekontorOrgnr,
        legekontorTlf = legekontorTlf,
    )

fun SykInnSykmeldingValues.toSykmeldingDocumentValues(): BehandlerSykmeldingValues {
    return BehandlerSykmeldingValues(
        hoveddiagnose = this.hoveddiagnose.toExistingSykmeldingDiagnoseInfo(),
        bidiagnoser = this.bidiagnoser.toExistingSykmeldingDiagnoseInfo(),
        aktivitet = this.aktivitet.toPersistedSykmeldingAktivitetToExistingSykmeldingAktivitet(),
        svangerskapsrelatert = this.svangerskapsrelatert,
        pasientenSkalSkjermes = this.pasientenSkalSkjermes,
        meldinger = this.meldinger.toExistingSykmeldingMeldinger(),
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
            // TODO: kor e tekst
            text = "TODO",
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
    BehandlerSykmeldingDiagnoseInfo(
        system = system,
        code = code,
        // TODO: kor e tekst
        text = "TODO",
    )
