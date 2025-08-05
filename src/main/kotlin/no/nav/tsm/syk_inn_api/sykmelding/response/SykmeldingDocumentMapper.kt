package no.nav.tsm.syk_inn_api.sykmelding.response

import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingYrkesskade

fun SykmeldingDocument.toLightSykmelding(): SykmeldingDocumentLight {
    return SykmeldingDocumentLight(
        sykmeldingId = this.sykmeldingId,
        meta = this.meta,
        values =
            SykmeldingDocumentLightValues(
                aktivitet =
                    this.values.aktivitet.map { aktivitet ->
                        SykmeldingDocumentLightAktivitet(
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                            type =
                                when (aktivitet) {
                                    is SykmeldingDocumentAktivitet.IkkeMulig ->
                                        "AKTIVITET_IKKE_MULIG"
                                    is SykmeldingDocumentAktivitet.Gradert -> "GRADERT"
                                    is SykmeldingDocumentAktivitet.Behandlingsdager ->
                                        "BEHANDLINGSDAGER"
                                    is SykmeldingDocumentAktivitet.Avventende -> "AVVENTENDE"
                                    is SykmeldingDocumentAktivitet.Reisetilskudd -> "REISETILSKUDD"
                                },
                        )
                    },
            ),
        utfall = this.utfall,
    )
}

fun PersistedSykmeldingSykmelder.toSykmeldingDocumentSykmelder(): SykmeldingDocumentSykmelder {
    return SykmeldingDocumentSykmelder(
        hprNummer = this.hprNummer,
        fornavn = this.fornavn,
        mellomnavn = this.mellomnavn,
        etternavn = this.etternavn,
    )
}

fun PersistedSykmelding.toSykmeldingDocumentValues(): SykmeldingDocumentValues {
    return SykmeldingDocumentValues(
        hoveddiagnose = this.hoveddiagnose.toExistingSykmeldingDiagnoseInfo(),
        bidiagnoser = this.bidiagnoser.toExistingSykmeldingDiagnoseInfo(),
        aktivitet = this.aktivitet.toPersistedSykmeldingAktivitetToExistingSykmeldingAktivitet(),
        svangerskapsrelatert = this.svangerskapsrelatert,
        pasientenSkalSkjermes = this.pasientenSkalSkjermes,
        meldinger = this.meldinger.toExistingSykmeldingMeldinger(),
        yrkesskade = this.yrkesskade.toExistingSykmeldingYrkesskade(),
        arbeidsgiver = this.arbeidsgiver.toExistingSykmeldingArbeidsgiver(),
        tilbakedatering = this.tilbakedatering.toExistingSykmeldingTilbakedatering(),
    )
}

private fun List<PersistedSykmeldingAktivitet>
    .toPersistedSykmeldingAktivitetToExistingSykmeldingAktivitet():
    List<SykmeldingDocumentAktivitet> {
    val aktiviteter = mutableListOf<SykmeldingDocumentAktivitet>()

    this.forEach { aktivitet ->
        when (aktivitet) {
            is PersistedSykmeldingAktivitet.IkkeMulig ->
                aktiviteter.add(
                    SykmeldingDocumentAktivitet.IkkeMulig(
                        fom = aktivitet.fom,
                        tom = aktivitet.tom,
                        medisinskArsak =
                            SykmeldingDocumentMedisinskArsak(
                                isMedisinskArsak = aktivitet.medisinskArsak.isMedisinskArsak,
                            ),
                        arbeidsrelatertArsak =
                            SykmeldingDocumentArbeidsrelatertArsak(
                                isArbeidsrelatertArsak =
                                    aktivitet.arbeidsrelatertArsak.isArbeidsrelatertArsak,
                                arbeidsrelaterteArsaker =
                                    aktivitet.arbeidsrelatertArsak.arbeidsrelaterteArsaker,
                                annenArbeidsrelatertArsak =
                                    aktivitet.arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                            ),
                    ),
                )
            is PersistedSykmeldingAktivitet.Gradert ->
                aktiviteter.add(
                    SykmeldingDocumentAktivitet.Gradert(
                        grad = aktivitet.grad,
                        fom = aktivitet.fom,
                        tom = aktivitet.tom,
                        reisetilskudd = aktivitet.reisetilskudd,
                    ),
                )
            is PersistedSykmeldingAktivitet.Behandlingsdager ->
                aktiviteter.add(
                    SykmeldingDocumentAktivitet.Behandlingsdager(
                        antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                        fom = aktivitet.fom,
                        tom = aktivitet.tom,
                    ),
                )
            is PersistedSykmeldingAktivitet.Avventende ->
                aktiviteter.add(
                    SykmeldingDocumentAktivitet.Avventende(
                        innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver,
                        fom = aktivitet.fom,
                        tom = aktivitet.tom,
                    ),
                )
            is PersistedSykmeldingAktivitet.Reisetilskudd ->
                aktiviteter.add(
                    SykmeldingDocumentAktivitet.Reisetilskudd(
                        fom = aktivitet.fom,
                        tom = aktivitet.tom,
                    ),
                )
        }
    }
    return aktiviteter
}

private fun List<PersistedSykmeldingDiagnoseInfo>.toExistingSykmeldingDiagnoseInfo():
    List<SykmeldingDocumentDiagnoseInfo>? {
    if (this.isEmpty()) return null

    return this.map { diagnose ->
        SykmeldingDocumentDiagnoseInfo(
            system = diagnose.system,
            code = diagnose.code,
            text = diagnose.text,
        )
    }
}

private fun PersistedSykmeldingTilbakedatering?.toExistingSykmeldingTilbakedatering():
    SykmeldingDocumentTilbakedatering? {
    return this?.let {
        SykmeldingDocumentTilbakedatering(
            startdato = it.startdato,
            begrunnelse = it.begrunnelse,
        )
    }
}

private fun PersistedSykmeldingArbeidsgiver?.toExistingSykmeldingArbeidsgiver():
    SykmeldingDocumentArbeidsgiver? {
    return this?.let {
        SykmeldingDocumentArbeidsgiver(
            harFlere = it.harFlere,
            arbeidsgivernavn = it.arbeidsgivernavn,
        )
    }
}

private fun PersistedSykmeldingYrkesskade?.toExistingSykmeldingYrkesskade():
    SykmeldingDocumentYrkesskade? {
    return this?.let {
        SykmeldingDocumentYrkesskade(
            yrkesskade = it.yrkesskade,
            skadedato = it.skadedato,
        )
    }
}

private fun PersistedSykmeldingMeldinger.toExistingSykmeldingMeldinger():
    SykmeldingDocumentMeldinger {
    return SykmeldingDocumentMeldinger(
        tilNav = this.tilNav,
        tilArbeidsgiver = this.tilArbeidsgiver,
    )
}

private fun PersistedSykmeldingDiagnoseInfo?.toExistingSykmeldingDiagnoseInfo():
    SykmeldingDocumentDiagnoseInfo? {
    return this?.let {
        SykmeldingDocumentDiagnoseInfo(
            system = it.system,
            code = it.code,
            text = it.text,
        )
    }
}
