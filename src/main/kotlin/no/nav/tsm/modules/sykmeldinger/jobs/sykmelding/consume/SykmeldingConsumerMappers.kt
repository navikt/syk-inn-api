package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import java.time.LocalDate
import java.util.*
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.modules.sykmeldinger.domain.*
import no.nav.tsm.sykmelding.input.core.model.*

fun SykmeldingRecord.toVerifiedSykmelding(): VerifiedSykInnSykmelding {
    return VerifiedSykInnSykmelding(
        sykmeldingId = UUID.fromString(sykmelding.id),
        values = toSykmeldingValues(),
        meta = toMetadata(),
        result = validation.toResult(),
    )
}

private fun ValidationResult.toResult(): SykInnSykmeldingRuleResult =
    when (this.status) {
        RuleType.OK -> SykInnSykmeldingRuleResult.OK()
        RuleType.PENDING,
        RuleType.INVALID -> {
            when (val rule: Rule = this.rules.first()) {
                is OKRule -> SykInnSykmeldingRuleResult.OK()
                is InvalidRule ->
                    SykInnSykmeldingRuleResult.Outcome(
                        type = status,
                        rule = rule.name,
                        message = rule.reason.sykmelder,
                    )
                is PendingRule ->
                    SykInnSykmeldingRuleResult.Outcome(
                        type = status,
                        rule = rule.name,
                        message = rule.reason.sykmelder,
                    )
            }
        }
    }

private fun SykmeldingRecord.toMetadata(): SykInnSykmeldingMeta {
    TODO("Not yet implemented")
}

private fun Aktivitet.toSykInnAktivitet(): SykInnAktivitet {
    return when (this) {
        is AktivitetIkkeMulig ->
            SykInnAktivitet.IkkeMulig(
                fom = this.fom,
                tom = this.tom,
                arbeidsrelatertArsak =
                    this.arbeidsrelatertArsak.let { arsak ->
                        SykInnArbeidsrelatertArsak(
                            isArbeidsrelatertArsak = arsak != null,
                            arbeidsrelaterteArsaker = arsak?.arsak ?: emptyList(),
                            annenArbeidsrelatertArsak = arsak?.beskrivelse,
                        )
                    },
            )

        is Avventende -> TODO()
        is Behandlingsdager -> TODO()
        is Gradert -> TODO()
        is Reisetilskudd -> TODO()
    }
}

private fun SykmeldingRecord.toSykmeldingValues(): SykInnSykmeldingValues {
    return SykInnSykmeldingValues(
        pasientenSkalSkjermes = sykmelding.medisinskVurdering.skjermetForPasient,
        hoveddiagnose = sykmelding.medisinskVurdering.hovedDiagnose?.toSykInnDiagnoseInfo(),
        bidiagnoser =
            (sykmelding.medisinskVurdering.biDiagnoser ?: emptyList()).map {
                it.toSykInnDiagnoseInfo()
            },
        aktivitet = sykmelding.aktivitet.map { it.toSykInnAktivitet() },
        svangerskapsrelatert = sykmelding.medisinskVurdering.svangerskap,
        meldinger = toMeldinger(meldingTilArbeidsgiver(), meldingTilNav()),
        yrkesskade = sykmelding.medisinskVurdering.yrkesskade?.toSykInnYrkesskade(),
        arbeidsgiver = sykmelding.toArbeidsgiver(),
        tilbakedatering = toTilbakedatering(kontaktDato(), tilbakedatertBegrunnelse()),
        utdypendeSporsmal = TODO(),
        annenFravarsgrunn = sykmelding.toSykInnFravarsGrunn(),
    )
}

private fun Sykmelding.toSykInnFravarsGrunn(): AnnenFravarsgrunn? {
    return when (val vurdering = this.medisinskVurdering) {
        is DigitalMedisinskVurdering -> vurdering.annenFravarsgrunn
        is LegacyMedisinskVurdering -> vurdering.annenFraversArsak?.arsak?.firstOrNull()
    }
}

private fun SykmeldingRecord.toTilbakedatering(
    kontaktDato: LocalDate?,
    tilbakedatertBegrunnelse: String?,
): SykInnTilbakedatering? {
    if (kontaktDato == null && tilbakedatertBegrunnelse != null) return null

    return SykInnTilbakedatering(kontaktDato, tilbakedatertBegrunnelse)
}

private fun SykmeldingRecord.kontaktDato(): LocalDate? {
    return when (val sykmelding = sykmelding) {
        is DigitalSykmelding -> sykmelding.tilbakedatering?.kontaktDato
        is Papirsykmelding -> sykmelding.tilbakedatering?.kontaktDato
        is XmlSykmelding -> sykmelding.tilbakedatering?.kontaktDato
        is UtenlandskSykmelding -> null
    }
}

private fun SykmeldingRecord.tilbakedatertBegrunnelse(): String? {
    return when (val sykmelding = sykmelding) {
        is DigitalSykmelding -> sykmelding.tilbakedatering?.begrunnelse
        is Papirsykmelding -> sykmelding.tilbakedatering?.begrunnelse
        is XmlSykmelding -> sykmelding.tilbakedatering?.begrunnelse
        is UtenlandskSykmelding -> null
    }
}

private fun Sykmelding.toTilbakedatering(): SykInnTilbakedatering? {
    return when (this) {
        is DigitalSykmelding ->
            SykInnTilbakedatering(
                this.tilbakedatering?.kontaktDato,
                this.tilbakedatering?.begrunnelse,
            )

        is Papirsykmelding ->
            SykInnTilbakedatering(
                this.tilbakedatering?.kontaktDato,
                this.tilbakedatering?.begrunnelse,
            )

        is XmlSykmelding ->
            SykInnTilbakedatering(
                this.tilbakedatering?.kontaktDato,
                this.tilbakedatering?.begrunnelse,
            )

        is UtenlandskSykmelding -> null
    }
}

private fun Sykmelding.toArbeidsgiver(): SykInnArbeidsgiver? {
    return when (this) {
        is Papirsykmelding -> this.arbeidsgiver.toSykInnArbeidsgiver()
        is DigitalSykmelding -> this.arbeidsgiver.toSykInnArbeidsgiver()
        is XmlSykmelding -> this.arbeidsgiver.toSykInnArbeidsgiver()
        is UtenlandskSykmelding -> null
    }
}

private fun ArbeidsgiverInfo.toSykInnArbeidsgiver(): SykInnArbeidsgiver? {
    return when (this) {
        is EnArbeidsgiver -> SykInnArbeidsgiver(false, this.navn)
        is FlereArbeidsgivere -> SykInnArbeidsgiver(false, this.navn)
        is IngenArbeidsgiver -> null
    }
}

private fun Yrkesskade.toSykInnYrkesskade(): SykInnYrkesskade {
    return SykInnYrkesskade(true, this.yrkesskadeDato)
}

private fun SykmeldingRecord.meldingTilNav(): String? {
    return when (val sykmelding = sykmelding) {
        is UtenlandskSykmelding -> null
        is DigitalSykmelding -> sykmelding.bistandNav?.beskrivBistand
        is Papirsykmelding -> sykmelding.bistandNav?.beskrivBistand
        is XmlSykmelding -> sykmelding.bistandNav?.beskrivBistand
    }
}

private fun SykmeldingRecord.meldingTilArbeidsgiver(): String? {
    return when (val sykmelding = sykmelding) {
        is UtenlandskSykmelding -> null
        is DigitalSykmelding -> getMeldingTilArbeidsgiver(sykmelding.arbeidsgiver)
        is Papirsykmelding -> getMeldingTilArbeidsgiver(sykmelding.arbeidsgiver)
        is XmlSykmelding -> getMeldingTilArbeidsgiver(sykmelding.arbeidsgiver)
    }
}

private fun getMeldingTilArbeidsgiver(arbeidsgiver: ArbeidsgiverInfo): String? =
    when (arbeidsgiver) {
        is EnArbeidsgiver -> arbeidsgiver.meldingTilArbeidsgiver
        is FlereArbeidsgivere -> arbeidsgiver.meldingTilArbeidsgiver
        is IngenArbeidsgiver -> null
    }

private fun SykmeldingRecord.toMeldinger(
    meldingTilArbeidsgiver: String?,
    meldingTilNav: String?,
): SykInnMeldinger? {
    if (meldingTilArbeidsgiver == null && meldingTilNav == null) return null

    return SykInnMeldinger(tilNav = meldingTilNav, tilArbeidsgiver = meldingTilArbeidsgiver)
}

private fun DiagnoseInfo.toSykInnDiagnoseInfo(): SykInnDiagnoseInfo =
    SykInnDiagnoseInfo(
        system =
            when (this.system) {
                DiagnoseSystem.ICPC2 -> SykInnDiagnoseSystem.ICPC2
                DiagnoseSystem.ICD10 -> SykInnDiagnoseSystem.ICD10
                DiagnoseSystem.ICPC2B -> SykInnDiagnoseSystem.ICPC2B
                else -> {
                    throw IllegalArgumentException("Unsupported system: ${this.system}")
                }
            },
        code = kode,
    )
