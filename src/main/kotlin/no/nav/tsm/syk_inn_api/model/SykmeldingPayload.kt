package no.nav.tsm.syk_inn_api.model

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo

data class SykmeldingPayload(
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: Sykmelding
)

data class Sykmelding(val hoveddiagnose: Hoveddiagnose, val aktivitet: Aktivitet)

@JsonSubTypes(
    JsonSubTypes.Type(Aktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(Aktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(Aktivitet.Ugyldig::class, name = "UGYLDIG"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Aktivitet {
    data class IkkeMulig(val fom: String, val tom: String) : Aktivitet

    data class Gradert(val grad: Int, val fom: String, val tom: String) : Aktivitet

    data class Ugyldig(val fom: String, val tom: String) : Aktivitet
}

enum class DiagnoseSystem {
    ICD10,
    ICPC2
}

data class Hoveddiagnose(
    val system: DiagnoseSystem,
    val code: String,
)
