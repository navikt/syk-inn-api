package no.nav.tsm.modules.sykmeldinger.db.exposed

import no.nav.tsm.modules.sykmeldinger.domain.SykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.domain.SykInnArbeidsrelatertArsak
import no.nav.tsm.modules.sykmeldinger.domain.SykInnDiagnoseInfo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnMedisinskArsak
import no.nav.tsm.modules.sykmeldinger.domain.SykInnMeldinger
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.text
import no.nav.tsm.sykmelding.input.core.model.RuleType

fun SykInnSykmeldingRuleResult.toRuleResultJson(): SykmeldingJsonbRuleResult =
    when (this) {
        is SykInnSykmeldingRuleResult.OK -> SykmeldingJsonbRuleResult(RuleType.OK, null, null)
        is SykInnSykmeldingRuleResult.Outcome ->
            SykmeldingJsonbRuleResult(type = this.type, message = this.message, rule = this.rule)
    }

fun SykInnDiagnoseInfo?.toDiagnoseJsonb(): SykmeldingJsonbDiagnose? =
    this?.let { SykmeldingJsonbDiagnose(system = it.system.name, text = it.text(), code = it.code) }

fun SykInnMeldinger?.toMeldingerJsonb(): SykmeldingJsonbMeldinger? =
    this?.let { SykmeldingJsonbMeldinger(tilNav = tilNav, tilArbeidsgiver = tilArbeidsgiver) }

fun SykInnAktivitet.toAktivitetJsonb(): SykmeldingJsonbAktivitet =
    when (this) {
        is SykInnAktivitet.IkkeMulig ->
            SykmeldingJsonbAktivitet.IkkeMulig(
                fom = fom,
                tom = tom,
                medisinskArsak = SykmeldingJsonbMedisinskArsak(medisinskArsak.isMedisinskArsak),
                arbeidsrelatertArsak =
                    SykmeldingJsonbArbeidsrelatertArsak(
                        isArbeidsrelatertArsak = arbeidsrelatertArsak.isArbeidsrelatertArsak,
                        arbeidsrelaterteArsaker = arbeidsrelatertArsak.arbeidsrelaterteArsaker,
                        annenArbeidsrelatertArsak = arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                    ),
            )
        is SykInnAktivitet.Gradert ->
            SykmeldingJsonbAktivitet.Gradert(
                grad = grad,
                fom = fom,
                tom = tom,
                reisetilskudd = reisetilskudd,
            )
        is SykInnAktivitet.Behandlingsdager ->
            SykmeldingJsonbAktivitet.Behandlingsdager(
                antallBehandlingsdager = antallBehandlingsdager,
                fom = fom,
                tom = tom,
            )
        is SykInnAktivitet.Avventende ->
            SykmeldingJsonbAktivitet.Avventende(
                innspillTilArbeidsgiver = innspillTilArbeidsgiver,
                fom = fom,
                tom = tom,
            )
        is SykInnAktivitet.Reisetilskudd ->
            SykmeldingJsonbAktivitet.Reisetilskudd(fom = fom, tom = tom)
    }

fun SykmeldingJsonbAktivitet.toSykInnAktivitet(): SykInnAktivitet =
    when (this) {
        is SykmeldingJsonbAktivitet.IkkeMulig ->
            SykInnAktivitet.IkkeMulig(
                fom = fom,
                tom = tom,
                medisinskArsak = SykInnMedisinskArsak(medisinskArsak.isMedisinskArsak),
                arbeidsrelatertArsak =
                    SykInnArbeidsrelatertArsak(
                        isArbeidsrelatertArsak = arbeidsrelatertArsak.isArbeidsrelatertArsak,
                        arbeidsrelaterteArsaker = arbeidsrelatertArsak.arbeidsrelaterteArsaker,
                        annenArbeidsrelatertArsak = arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                    ),
            )
        is SykmeldingJsonbAktivitet.Gradert ->
            SykInnAktivitet.Gradert(
                grad = grad,
                fom = fom,
                tom = tom,
                reisetilskudd = reisetilskudd,
            )
        is SykmeldingJsonbAktivitet.Behandlingsdager ->
            SykInnAktivitet.Behandlingsdager(
                antallBehandlingsdager = antallBehandlingsdager,
                fom = fom,
                tom = tom,
            )
        is SykmeldingJsonbAktivitet.Avventende ->
            SykInnAktivitet.Avventende(
                innspillTilArbeidsgiver = innspillTilArbeidsgiver,
                fom = fom,
                tom = tom,
            )
        is SykmeldingJsonbAktivitet.Reisetilskudd ->
            SykInnAktivitet.Reisetilskudd(fom = fom, tom = tom)
    }
