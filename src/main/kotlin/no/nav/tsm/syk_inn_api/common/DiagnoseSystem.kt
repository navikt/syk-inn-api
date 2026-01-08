package no.nav.tsm.syk_inn_api.common

import no.nav.tsm.diagnoser.ICD10
import no.nav.tsm.diagnoser.ICPC2
import no.nav.tsm.diagnoser.ICPC2B

enum class DiagnoseSystem {
    ICPC2,
    ICD10,
    ICPC2B,
    PHBU,
    UGYLDIG
}

object DiagnosekodeMapper {
    fun findTextFromDiagnoseSystem(system: DiagnoseSystem, code: String): String? =
        when (system) {
            DiagnoseSystem.ICD10 -> ICD10[code]?.text
            DiagnoseSystem.ICPC2 -> ICPC2[code]?.text
            DiagnoseSystem.ICPC2B -> ICPC2B[code]?.text
            else -> null
        }
}
