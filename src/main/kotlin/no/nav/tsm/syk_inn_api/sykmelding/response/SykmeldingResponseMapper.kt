package no.nav.tsm.syk_inn_api.sykmelding.response

import no.nav.tsm.syk_inn_api.persistence.PersistedSykmelding
import no.nav.tsm.syk_inn_api.persistence.PersistedSykmeldingAktivitet

object SykmeldingResponseMapper {

    fun mapPersistedSykmeldingToExistingSykmelding(
        persistedSykmelding: PersistedSykmelding
    ): ExistingSykmelding {
        return ExistingSykmelding(
            hoveddiagnose =
                ExistingSykmeldingHoveddiagnose(
                    system = persistedSykmelding.hoveddiagnose.system,
                    code = persistedSykmelding.hoveddiagnose.code,
                    text = persistedSykmelding.hoveddiagnose.text,
                ),
            aktivitet =
                mapPersistedSykmeldingAktivitetToExistingSykmeldingAktivitet(
                    persistedSykmelding.aktivitet
                ),
        )
    }

    private fun mapPersistedSykmeldingAktivitetToExistingSykmeldingAktivitet(
        persistedSykmeldingAktivitet: PersistedSykmeldingAktivitet
    ): ExistingSykmeldingAktivitet {
        return when (persistedSykmeldingAktivitet) {
            is PersistedSykmeldingAktivitet.IkkeMulig ->
                ExistingSykmeldingAktivitet.IkkeMulig(
                    fom = persistedSykmeldingAktivitet.fom,
                    tom = persistedSykmeldingAktivitet.tom,
                )
            is PersistedSykmeldingAktivitet.Gradert ->
                ExistingSykmeldingAktivitet.Gradert(
                    grad = persistedSykmeldingAktivitet.grad,
                    fom = persistedSykmeldingAktivitet.fom,
                    tom = persistedSykmeldingAktivitet.tom,
                    reisetilskudd = persistedSykmeldingAktivitet.reisetilskudd,
                )
            is PersistedSykmeldingAktivitet.Behandlingsdager ->
                ExistingSykmeldingAktivitet.Behandlingsdager(
                    antallBehandlingsdager = persistedSykmeldingAktivitet.antallBehandlingsdager,
                    fom = persistedSykmeldingAktivitet.fom,
                    tom = persistedSykmeldingAktivitet.tom,
                )
            is PersistedSykmeldingAktivitet.Avventende ->
                ExistingSykmeldingAktivitet.Avventende(
                    innspillTilArbeidsgiver = persistedSykmeldingAktivitet.innspillTilArbeidsgiver,
                    fom = persistedSykmeldingAktivitet.fom,
                    tom = persistedSykmeldingAktivitet.tom,
                )
            is PersistedSykmeldingAktivitet.Reisetilskudd ->
                ExistingSykmeldingAktivitet.Reisetilskudd(
                    fom = persistedSykmeldingAktivitet.fom,
                    tom = persistedSykmeldingAktivitet.tom,
                )
        }
    }
}
