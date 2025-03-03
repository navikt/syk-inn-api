package no.nav.tsm.sykinnapi.modell.syfosmregister

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

data class SykInnSykmeldingDTO(
    val sykmeldingId: String,
    val aktivitet: Aktivitet,
    val pasient: Pasient,
    val hovedDiagnose: Diagnose,
    val behandler: Behandler
)

@JsonSubTypes(
    JsonSubTypes.Type(Aktivitet.AktivitetIkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(Aktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(Aktivitet.Avvetende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(Aktivitet.Behandlingsdager::class, name = "BEHANDLINGSDAGER"),
    JsonSubTypes.Type(Aktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Aktivitet {
    data class AktivitetIkkeMulig(val fom: LocalDate, val tom: LocalDate) : Aktivitet

    data class Gradert(val grad: Int, val fom: LocalDate, val tom: LocalDate) : Aktivitet

    data class Avvetende(val fom: LocalDate, val tom: LocalDate) : Aktivitet

    data class Behandlingsdager(val fom: LocalDate, val tom: LocalDate) : Aktivitet

    data class Reisetilskudd(val fom: LocalDate, val tom: LocalDate) : Aktivitet
}

data class Pasient(val fnr: String)

data class Behandler(val fnr: String, val hpr: String?)

data class Diagnose(val code: String, val system: String, val text: String)
