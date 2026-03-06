package modules.behandler.api.mappers

import modules.behandler.api.payloads.OpprettSykmelding
import modules.sykmeldinger.SykInnAktivitet
import modules.sykmeldinger.SykInnArbeidsgiver
import modules.sykmeldinger.SykInnArbeidsrelatertArsak
import modules.sykmeldinger.SykInnDiagnoseInfo
import modules.sykmeldinger.SykInnMedisinskArsak
import modules.sykmeldinger.SykInnMeldinger
import modules.sykmeldinger.SykInnSykmelding
import modules.sykmeldinger.SykInnSykmeldingValues
import modules.sykmeldinger.SykInnTilbakedatering
import modules.sykmeldinger.SykInnUtdypendeSporsmal
import modules.sykmeldinger.SykInnUtdypendeSporsmalSvar
import modules.sykmeldinger.SykInnYrkesskade

fun OpprettSykmelding.Payload.toSykInnSykmelding(): SykInnSykmelding {
    return SykInnSykmelding(
        sykmeldingId = this.submitId,
        values =
            SykInnSykmeldingValues(
                pasientenSkalSkjermes = this.values.pasientenSkalSkjermes,
                hoveddiagnose =
                    SykInnDiagnoseInfo(
                        system = this.values.hoveddiagnose.system,
                        code = this.values.hoveddiagnose.code,
                    ),
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
                            startdato = it.startdato,
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
                medisinskArsak =
                    SykInnMedisinskArsak(isMedisinskArsak = this.medisinskArsak.isMedisinskArsak),
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
