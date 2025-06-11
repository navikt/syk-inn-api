package no.nav.tsm.syk_inn_api.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.helse.diagnosekoder.Diagnosekoder

enum class DiagnoseSystem(val code: String) {
    ICPC2("ICPC2"),
    ICD10("ICD10");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromOid(value: String): DiagnoseSystem =
            entries.find { it.code == value }
                ?: throw IllegalArgumentException("Unknown DiagnoseSystem, code: $value")
    }

    @JsonValue fun toJson(): String = code
}

object DiagnosekodeMapper {
    fun findTextFromDiagnoseSystem(system: DiagnoseSystem, code: String): String? =
        when (system) {
            DiagnoseSystem.ICD10 -> Diagnosekoder.icd10[code]?.text
            DiagnoseSystem.ICPC2 -> Diagnosekoder.icpc2[code]?.text
        }
}
