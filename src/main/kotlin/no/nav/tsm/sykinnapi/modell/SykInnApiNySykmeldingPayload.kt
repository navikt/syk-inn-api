package no.nav.tsm.sykinnapi.modell

data class SykInnApiNySykmeldingPayload(
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: Sykmelding
)

data class Sykmelding(val hoveddiagnose: Hoveddiagnose, val aktivitet: Aktivitet)

sealed class Aktivitet {
    data class AktivitetIkkeMulig(val fom: String, val tom: String) : Aktivitet()

    data class Gradert(val grad: Int, val fom: String, val tom: String) : Aktivitet()
}

enum class DiagnoseSystem {
    ICD10,
    ICPC2
}

data class Hoveddiagnose(
    val system: DiagnoseSystem,
    val code: String,
)
