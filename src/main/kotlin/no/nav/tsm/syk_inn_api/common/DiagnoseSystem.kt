package no.nav.tsm.syk_inn_api.common

import no.nav.helse.diagnosekoder.Diagnosekoder

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
            DiagnoseSystem.ICD10 -> Diagnosekoder.icd10[code]?.text
            DiagnoseSystem.ICPC2 -> Diagnosekoder.icpc2[code]?.text
            else -> null
        }
}
