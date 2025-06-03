package no.nav.tsm.syk_inn_api.sykmeldingresponse

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem

data class SykmeldingResponse(
    val sykmeldingId: String,
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: ExistingSykmelding,
    val legekontorOrgnr: String,
)

data class ExistingSykmelding(
    val hoveddiagnose: ExistingSykmeldingHoveddiagnose,
    val aktivitet: ExistingSykmeldingAktivitet
)

@JsonSubTypes(
    JsonSubTypes.Type(ExistingSykmeldingAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(ExistingSykmeldingAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(ExistingSykmeldingAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(
        ExistingSykmeldingAktivitet.Behandlingsdager::class,
        name = "BEHANDLINGSDAGER"
    ),
    JsonSubTypes.Type(ExistingSykmeldingAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface ExistingSykmeldingAktivitet {
    data class IkkeMulig(val fom: String, val tom: String) : ExistingSykmeldingAktivitet

    data class Gradert(
        val grad: Int,
        val fom: String,
        val tom: String,
        val reisetilskudd: Boolean
    ) : ExistingSykmeldingAktivitet

    data class Behandlingsdager(val antallBehandlingsdager: Int, val fom: String, val tom: String) :
        ExistingSykmeldingAktivitet

    data class Avventende(val innspillTilArbeidsgiver: String, val fom: String, val tom: String) :
        ExistingSykmeldingAktivitet

    data class Reisetilskudd(val fom: String, val tom: String) : ExistingSykmeldingAktivitet
}

data class ExistingSykmeldingHoveddiagnose(
    val system: DiagnoseSystem,
    val code: String,
    val text: String,
)
