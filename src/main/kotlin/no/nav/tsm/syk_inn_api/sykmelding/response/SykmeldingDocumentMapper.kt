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
    ): SykmeldingDocumentValues {
        return SykmeldingDocumentValues(
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
        List<SykmeldingDocumentAktivitet> {
        val aktiviteter = mutableListOf<SykmeldingDocumentAktivitet>()

        this.forEach { aktivitet ->
            when (aktivitet) {
                is PersistedSykmeldingAktivitet.IkkeMulig ->
                    aktiviteter.add(
                        SykmeldingDocumentAktivitet.IkkeMulig(
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                        )
                    )
                is PersistedSykmeldingAktivitet.Gradert ->
                    aktiviteter.add(
                        SykmeldingDocumentAktivitet.Gradert(
                            grad = aktivitet.grad,
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                            reisetilskudd = aktivitet.reisetilskudd,
                        )
                    )
                is PersistedSykmeldingAktivitet.Behandlingsdager ->
                    aktiviteter.add(
                        SykmeldingDocumentAktivitet.Behandlingsdager(
                            antallBehandlingsdager = aktivitet.antallBehandlingsdager,
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                        )
                    )
                is PersistedSykmeldingAktivitet.Avventende ->
                    aktiviteter.add(
                        SykmeldingDocumentAktivitet.Avventende(
                            innspillTilArbeidsgiver = aktivitet.innspillTilArbeidsgiver,
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                        )
                    )
                is PersistedSykmeldingAktivitet.Reisetilskudd ->
                    aktiviteter.add(
                        SykmeldingDocumentAktivitet.Reisetilskudd(
                            fom = aktivitet.fom,
                            tom = aktivitet.tom,
                        )
                    )
            }
        }
        return aktiviteter
    }

    private fun List<PersistedSykmeldingDiagnoseInfo>.toExistingSykmeldingDiagnoseInfo():
        List<SykmeldingDocumentDiagnoseInfo> {
        if (this.isEmpty()) return emptyList()

        return this.map { diagnose ->
            SykmeldingDocumentDiagnoseInfo(
                system = diagnose.system,
                code = diagnose.code,
                text = diagnose.text,
            )
        }
    }

    private fun PersistedSykmeldingRuleResult.toExistingSykmeldingRuleResult():
        SykmeldingDocumentRuleResult {
        return SykmeldingDocumentRuleResult(
            result = this.result,
            melding = this.meldingTilSender,
        )
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
}
