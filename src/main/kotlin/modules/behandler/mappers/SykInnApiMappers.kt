package modules.behandler.mappers

import modules.behandler.payloads.BehandlerSykmeldingVerify
import modules.behandler.payloads.OpprettSykmelding
import modules.sykmeldinger.domain.SykInnAktivitet
import modules.sykmeldinger.domain.SykInnArbeidsgiver
import modules.sykmeldinger.domain.SykInnArbeidsrelatertArsak
import modules.sykmeldinger.domain.SykInnDiagnoseInfo
import modules.sykmeldinger.domain.SykInnMedisinskArsak
import modules.sykmeldinger.domain.SykInnMeldinger
import modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import modules.sykmeldinger.domain.SykInnSykmeldingValues
import modules.sykmeldinger.domain.SykInnTilbakedatering
import modules.sykmeldinger.domain.SykInnUtdypendeSporsmal
import modules.sykmeldinger.domain.SykInnUtdypendeSporsmalSvar
import modules.sykmeldinger.domain.SykInnYrkesskade
import modules.sykmeldinger.domain.UnverifiedSykInnSykmelding
import modules.sykmeldinger.domain.UnverifiedSykInnSykmeldingMeta
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.sykmelding.input.core.model.RuleType

fun SykInnSykmeldingRuleResult.Outcome.toBehandlerSykmeldingVerify(): BehandlerSykmeldingVerify =
    when (this) {
        is SykInnSykmeldingRuleResult.Outcome ->
            BehandlerSykmeldingVerify(
                status =
                    when (this.type) {
                        RuleType.PENDING -> RegulaOutcomeStatus.MANUAL_PROCESSING
                        RuleType.INVALID -> RegulaOutcomeStatus.INVALID
                        RuleType.OK ->
                            throw IllegalStateException(
                                "Outcome cannot be OK, only PENDING and INVALID should have message"
                            )
                    },
                message = this.message,
                rule = this.rule,
            )
    }

fun OpprettSykmelding.Payload.toSykInnSykmelding(): UnverifiedSykInnSykmelding {
    return UnverifiedSykInnSykmelding(
        sykmeldingId = this.submitId,
        meta =
            UnverifiedSykInnSykmeldingMeta(
                behandlerHpr = this.meta.sykmelderHpr,
                pasientIdent = this.meta.pasientIdent,
                legekontorOrgnr = this.meta.legekontorOrgnr,
                legekontorTlf = this.meta.legekontorTlf,
            ),
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
