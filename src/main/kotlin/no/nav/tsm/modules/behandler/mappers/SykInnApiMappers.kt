package no.nav.tsm.modules.behandler.mappers

import no.nav.tsm.modules.behandler.payloads.BehandlerSykmeldingVerify
import no.nav.tsm.modules.behandler.payloads.OpprettSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.SykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.domain.SykInnArbeidsgiver
import no.nav.tsm.modules.sykmeldinger.domain.SykInnArbeidsrelatertArsak
import no.nav.tsm.modules.sykmeldinger.domain.SykInnDiagnoseInfo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnMeldinger
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingValues
import no.nav.tsm.modules.sykmeldinger.domain.SykInnTilbakedatering
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmal
import no.nav.tsm.modules.sykmeldinger.domain.SykInnUtdypendeSporsmalSvar
import no.nav.tsm.modules.sykmeldinger.domain.SykInnYrkesskade
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.UnverifiedSykInnSykmeldingMeta
import no.nav.tsm.sykmelding.input.core.model.RuleType

fun SykInnSykmeldingRuleResult.toBehandlerSykmeldingVerify(): BehandlerSykmeldingVerify =
    when (this) {
        is SykInnSykmeldingRuleResult.OK ->
            BehandlerSykmeldingVerify(status = RuleType.OK, message = null, rule = null)

        is SykInnSykmeldingRuleResult.Outcome ->
            BehandlerSykmeldingVerify(status = this.type, message = this.message, rule = this.rule)
    }

fun OpprettSykmelding.Payload.toSykInnSykmelding(): UnverifiedSykInnSykmelding {
    return UnverifiedSykInnSykmelding(
        submitId = this.submitId,
        meta =
            UnverifiedSykInnSykmeldingMeta(
                source = this.meta.source,
                behandlerHpr = this.meta.sykmelderHpr,
                pasientIdent = this.meta.pasientIdent,
                legekontorOrgnr = this.meta.legekontorOrgnr,
                legekontorTlf = this.meta.legekontorTlf,
            ),
        values =
            SykInnSykmeldingValues(
                pasientenSkalSkjermes = this.values.pasientenSkalSkjermes,
                hoveddiagnose =
                    this.values.hoveddiagnose.let {
                        SykInnDiagnoseInfo(system = it.system, code = it.code)
                    },
                bidiagnoser =
                    this.values.bidiagnoser.map {
                        SykInnDiagnoseInfo(system = it.system, code = it.code)
                    },
                aktivitet = this.values.aktivitet.map { it.toSykInnApiAktivitet() },
                svangerskapsrelatert = this.values.svangerskapsrelatert,
                meldinger =
                    SykInnMeldinger(
                        tilNav = this.values.meldinger.tilNav,
                        tilArbeidsgiver = this.values.meldinger.tilArbeidsgiver,
                    ),
                yrkesskade =
                    this.values.yrkesskade?.let {
                        SykInnYrkesskade(yrkesskade = it.yrkesskade, skadedato = it.skadedato)
                    },
                arbeidsgiver =
                    this.values.arbeidsgiver?.let {
                        SykInnArbeidsgiver(
                            harFlere = it.harFlere,
                            arbeidsgivernavn = it.arbeidsgivernavn,
                        )
                    },
                tilbakedatering =
                    this.values.tilbakedatering?.let {
                        SykInnTilbakedatering(
                            kontaktdato = it.startdato,
                            begrunnelse = it.begrunnelse,
                        )
                    },
                utdypendeSporsmal = this.values.utdypendeSporsmal?.mapToSykInnUtdypendeSporsmal(),
                annenFravarsgrunn = this.values.annenFravarsgrunn,
            ),
    )
}

private fun OpprettSykmelding.UtdypendeSporsmal.mapToSykInnUtdypendeSporsmal():
    SykInnUtdypendeSporsmal {
    return SykInnUtdypendeSporsmal(
        hensynPaArbeidsplassen =
            this.hensynPaArbeidsplassen?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        medisinskOppsummering =
            this.medisinskOppsummering?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        utfordringerMedArbeid =
            this.utfordringerMedArbeid?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        sykdomsutvikling =
            this.sykdomsutvikling?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        arbeidsrelaterteUtfordringer =
            this.arbeidsrelaterteUtfordringer?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        behandlingOgFremtidigArbeid =
            this.behandlingOgFremtidigArbeid?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        uavklarteForhold =
            this.uavklarteForhold?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        oppdatertMedisinskStatus =
            this.oppdatertMedisinskStatus?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        realistiskMestringArbeid =
            this.realistiskMestringArbeid?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        forventetHelsetilstandUtvikling =
            this.forventetHelsetilstandUtvikling?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
        medisinskeHensyn =
            this.medisinskeHensyn?.let {
                SykInnUtdypendeSporsmalSvar(sporsmalstekst = it.sporsmalstekst, svar = it.svar)
            },
    )
}

private fun OpprettSykmelding.Aktivitet.toSykInnApiAktivitet(): SykInnAktivitet {
    return when (this) {
        is OpprettSykmelding.Aktivitet.Avventende ->
            SykInnAktivitet.Avventende(
                fom = this.fom,
                tom = this.tom,
                innspillTilArbeidsgiver = this.innspillTilArbeidsgiver,
            )

        is OpprettSykmelding.Aktivitet.Behandlingsdager ->
            SykInnAktivitet.Behandlingsdager(
                fom = this.fom,
                tom = this.tom,
                antallBehandlingsdager = this.antallBehandlingsdager,
            )

        is OpprettSykmelding.Aktivitet.Gradert ->
            SykInnAktivitet.Gradert(
                fom = this.fom,
                tom = this.tom,
                grad = this.grad,
                reisetilskudd = this.reisetilskudd,
            )

        is OpprettSykmelding.Aktivitet.IkkeMulig ->
            SykInnAktivitet.IkkeMulig(
                fom = this.fom,
                tom = this.tom,
                arbeidsrelatertArsak =
                    SykInnArbeidsrelatertArsak(
                        isArbeidsrelatertArsak = this.arbeidsrelatertArsak.isArbeidsrelatertArsak,
                        arbeidsrelaterteArsaker = this.arbeidsrelatertArsak.arbeidsrelaterteArsaker,
                        annenArbeidsrelatertArsak =
                            this.arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                    ),
            )

        is OpprettSykmelding.Aktivitet.Reisetilskudd ->
            SykInnAktivitet.Reisetilskudd(fom = this.fom, tom = this.tom)
    }
}
