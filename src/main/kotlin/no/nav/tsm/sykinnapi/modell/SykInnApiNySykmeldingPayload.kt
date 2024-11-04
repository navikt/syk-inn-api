package no.nav.tsm.sykinnapi.modell

data class SykInnApiNySykmeldingPayload(
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: Sykmelding
)

data class Sykmelding(val hoveddiagnose: Hoveddiagnose, val aktivitet: Aktivitet)

data class Aktivitet(val type: AktivitetType)

data class AktivitetIkkeMulig(val fom: String, val tom: String)

data class Gradert(val grad: Int, val fom: String, val tom: String)

enum class AktivitetType {
    AKTIVITET_IKKE_MULIG,
    GRADERT
}

enum class DiagnoseSystem {
    ICD10,
    ICPC2
}

data class Hoveddiagnose(
    val system: DiagnoseSystem,
    val code: String,
)
