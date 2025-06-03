package no.nav.tsm.syk_inn_api.persistence

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem

data class PersistedSykmelding(
    val hoveddiagnose: PersistedSykmeldingHoveddiagnose,
    val aktivitet: PersistedSykmeldingAktivitet
)

@JsonSubTypes(
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(
        PersistedSykmeldingAktivitet.Behandlingsdager::class,
        name = "BEHANDLINGSDAGER"
    ),
    JsonSubTypes.Type(PersistedSykmeldingAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface PersistedSykmeldingAktivitet {
    data class IkkeMulig(val fom: String, val tom: String) : PersistedSykmeldingAktivitet

    data class Gradert(
        val grad: Int,
        val fom: String,
        val tom: String,
        val reisetilskudd: Boolean
    ) : PersistedSykmeldingAktivitet

    data class Behandlingsdager(val antallBehandlingsdager: Int, val fom: String, val tom: String) :
        PersistedSykmeldingAktivitet

    data class Avventende(val innspillTilArbeidsgiver: String, val fom: String, val tom: String) :
        PersistedSykmeldingAktivitet

    data class Reisetilskudd(val fom: String, val tom: String) : PersistedSykmeldingAktivitet
}

data class PersistedSykmeldingHoveddiagnose(
    val system: DiagnoseSystem,
    val code: String,
    val text: String,
)
