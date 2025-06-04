package no.nav.tsm.syk_inn_api.sykmelding.response

import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingArbeidsgiver
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingTilbakedatering
import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedSykmeldingYrkesskade

object SykmeldingDocumentMapper {

    fun mapPersistedSykmeldingToSykmeldingDokument(
        persistedSykmelding: PersistedSykmelding
    ): ExistingSykmelding {
        return ExistingSykmelding(
            hoveddiagnose = persistedSykmelding.hoveddiagnose.toExistingSykmeldingDiagnoseInfo(),
            aktivitet =
                persistedSykmelding.aktivitet
                    .toPersistedSykmeldingAktivitetToExistingSykmeldingAktivitet(),
            bidiagnoser = persistedSykmelding.bidiagnoser.toExistingSykmeldingDiagnoseInfo(),
            svangerskapsrelatert = persistedSykmelding.svangerskapsrelatert,
            pasientenSkalSkjermes = persistedSykmelding.pasientenSkalSkjermes,
            meldinger = persistedSykmelding.meldinger.toExistingSykmeldingMeldinger(),
            yrkesskade = persistedSykmelding.yrkesskade.toExistingSykmeldingYrkesskade(),
            arbeidsgiver = persistedSykmelding.arbeidsgiver.toExistingSykmeldingArbeidsgiver(),
            tilbakedatering =
                persistedSykmelding.tilbakedatering.toExistingSykmeldingTilbakedatering(),
            regelResultat = persistedSykmelding.regelResultat.toExistingSykmeldingRuleResult(),
        )
    }

    private fun List<PersistedSykmeldingAktivitet>
        .toPersistedSykmeldingAktivitetToExistingSykmeldingAktivitet():
        List<ExistingSykmeldingAktivitet> {
        val aktiviteter = mutableListOf<ExistingSykmeldingAktivitet>()

        this.forEach { aktivitet ->
            when (aktivitet) {
                is PersistedSykmeldingAktivitet.IkkeMulig ->
                    aktiviteter.add(
                        ExistingSykmeldingAktivitet.IkkeMulig(
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                        )
                    )
                is PersistedSykmeldingAktivitet.Gradert ->
                    aktiviteter.add(
                        ExistingSykmeldingAktivitet.Gradert(
                            grad = aktivitet.grad,
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                            reisetilskudd = aktivitet.reisetilskudd,
                        )
                    )
                is PersistedSykmeldingAktivitet.Behandlingsdager ->
                    aktiviteter.add(
                        ExistingSykmeldingAktivitet.Behandlingsdager(
                            antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                        )
                    )
                is PersistedSykmeldingAktivitet.Avventende ->
                    aktiviteter.add(
                        ExistingSykmeldingAktivitet.Avventende(
                            innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver,
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                        )
                    )
                is PersistedSykmeldingAktivitet.Reisetilskudd ->
                    aktiviteter.add(
                        ExistingSykmeldingAktivitet.Reisetilskudd(
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                        )
                    )
            }
        }
        return aktiviteter
    }

    private fun List<PersistedSykmeldingDiagnoseInfo>.toExistingSykmeldingDiagnoseInfo():
        List<ExistingSykmeldingDiagnoseInfo> {
        if (this.isEmpty()) return emptyList()

        return this.map { diagnose ->
            ExistingSykmeldingDiagnoseInfo(
                system = diagnose.system,
                code = diagnose.code,
                text = diagnose.text,
            )
        }
    }

    private fun PersistedSykmeldingRuleResult.toExistingSykmeldingRuleResult():
        ExistingSykmeldingRuleResult {
        return ExistingSykmeldingRuleResult(
            result = this.result,
            meldingTilSender = this.meldingTilSender,
        )
    }

    private fun PersistedSykmeldingTilbakedatering?.toExistingSykmeldingTilbakedatering():
        ExistingSykmeldingTilbakedatering? {
        return this?.let {
            ExistingSykmeldingTilbakedatering(
                startdato = it.startdato,
                begrunnelse = it.begrunnelse,
            )
        }
    }

    private fun PersistedSykmeldingArbeidsgiver?.toExistingSykmeldingArbeidsgiver():
        ExistingSykmeldingArbeidsgiver? {
        return this?.let {
            ExistingSykmeldingArbeidsgiver(
                harFlere = it.harFlere,
                arbeidsgivernavn = it.arbeidsgivernavn,
            )
        }
    }

    private fun PersistedSykmeldingYrkesskade?.toExistingSykmeldingYrkesskade():
        ExistingSykmeldingYrkesskade? {
        return this?.let {
            ExistingSykmeldingYrkesskade(
                yrkesskade = it.yrkesskade,
                skadedato = it.skadedato,
            )
        }
    }

    private fun PersistedSykmeldingMeldinger.toExistingSykmeldingMeldinger():
        ExistingSykmeldingMeldinger {
        return ExistingSykmeldingMeldinger(
            tilNav = this.tilNav,
            tilArbeidsgiver = this.tilArbeidsgiver,
        )
    }

    private fun PersistedSykmeldingDiagnoseInfo?.toExistingSykmeldingDiagnoseInfo():
        ExistingSykmeldingDiagnoseInfo? {
        return this?.let {
            ExistingSykmeldingDiagnoseInfo(
                system = it.system,
                code = it.code,
                text = it.text,
            )
        }
    }
}
