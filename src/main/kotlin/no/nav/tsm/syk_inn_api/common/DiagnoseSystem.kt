package no.nav.tsm.syk_inn_api.common

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue
import no.nav.helse.diagnosekoder.Diagnosekoder
import org.slf4j.LoggerFactory

enum class DiagnoseSystem(val oid: String) {
    ICPC2("2.16.578.1.12.4.1.1.7170"),
    ICD10("2.16.578.1.12.4.1.1.7110");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromOid(value: String): DiagnoseSystem =
            values().find { it.oid == value }
                ?: throw IllegalArgumentException("Unknown DiagnoseSystem OID: $value")
    }

    @JsonValue fun toJson(): String = oid
}

object DiagnosekodeMapper {
    private val logger = LoggerFactory.getLogger(this::class.java)

    fun findTextFromDiagnoseSystem(system: String, code: String): String? {
        val diagnoseSystem =
            try {
                DiagnoseSystem.fromOid(system)
            } catch (iae: IllegalArgumentException) {
                logger.error("Unknown DiagnoseSystem OID: $system", iae)
                return null
            }

        return when (diagnoseSystem) {
            DiagnoseSystem.ICD10 -> Diagnosekoder.icd10[code]?.text
            DiagnoseSystem.ICPC2 -> Diagnosekoder.icpc2[code]?.text
        }
    }
}
