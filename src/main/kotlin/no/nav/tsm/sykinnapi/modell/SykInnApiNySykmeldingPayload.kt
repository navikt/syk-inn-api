package no.nav.tsm.sykinnapi.modell

import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.io.Serializable

data class SykInnApiNySykmeldingPayload(
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: Sykmelding
)

data class Sykmelding(val hoveddiagnose: Hoveddiagnose, val aktivitet: Aktivitet)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
sealed interface Aktivitet: Serializable
sealed class AktivitetType : Aktivitet
data class AktivitetIkkeMulig(val fom: String, val tom: String) : AktivitetType()
data class Gradert(val grad: Int, val fom: String, val tom: String) : AktivitetType()

enum class DiagnoseSystem {
    ICD10,
    ICPC2
}

data class Hoveddiagnose(
    val system: DiagnoseSystem,
    val code: String,
)

