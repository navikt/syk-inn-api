package no.nav.tsm.syk_inn_api.model.sykmelding

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem

data class SykmeldingPayload(
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: OpprettSykmeldingPayload,
    val legekontorOrgnr: String,
)

data class OpprettSykmeldingPayload(
    val hoveddiagnose: Hoveddiagnose,
    val opprettSykmeldingAktivitet: OpprettSykmeldingAktivitet
)

@JsonSubTypes(
    JsonSubTypes.Type(OpprettSykmeldingAktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(OpprettSykmeldingAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(OpprettSykmeldingAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(
        OpprettSykmeldingAktivitet.Behandlingsdager::class,
        name = "BEHANDLINGSDAGER"
    ),
    JsonSubTypes.Type(OpprettSykmeldingAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface OpprettSykmeldingAktivitet {
    data class IkkeMulig(val fom: String, val tom: String) : OpprettSykmeldingAktivitet

    data class Gradert(
        val grad: Int,
        val fom: String,
        val tom: String,
        val reisetilskudd: Boolean
    ) : OpprettSykmeldingAktivitet

    data class Behandlingsdager(val antallBehandlingsdager: Int, val fom: String, val tom: String) :
        OpprettSykmeldingAktivitet

    data class Avventende(val innspillTilArbeidsgiver: String, val fom: String, val tom: String) :
        OpprettSykmeldingAktivitet

    data class Reisetilskudd(val fom: String, val tom: String) : OpprettSykmeldingAktivitet
}

data class Hoveddiagnose(
    val system: DiagnoseSystem,
    val code: String,
)
