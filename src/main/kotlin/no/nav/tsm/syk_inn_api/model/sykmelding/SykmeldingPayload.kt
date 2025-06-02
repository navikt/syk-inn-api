package no.nav.tsm.syk_inn_api.model.sykmelding

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonValue

data class SykmeldingPayload(
    val pasientFnr: String,
    val sykmelderHpr: String,
    val sykmelding: Sykmelding,
    val legekontorOrgnr: String,
)

data class Sykmelding(val hoveddiagnose: Hoveddiagnose, val aktivitet: Aktivitet)

@JsonSubTypes(
    JsonSubTypes.Type(Aktivitet.IkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(Aktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(Aktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(Aktivitet.Behandlingsdager::class, name = "BEHANDLINGSDAGER"),
    JsonSubTypes.Type(Aktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Aktivitet {
    data class IkkeMulig(val fom: String, val tom: String) : Aktivitet
    //    val medisinskArsak: MedisinskArsak?, TODO desse typane må kanskje gjelde for IkkeMulig -
    // utvide på sikt.
    //    val arbeidsrelatertArsak: ArbeidsrelatertArsak?,

    data class Gradert(
        val grad: Int,
        val fom: String,
        val tom: String,
        val reisetilskudd: Boolean
    ) : Aktivitet

    data class Behandlingsdager(val antallBehandlingsdager: Int, val fom: String, val tom: String) :
        Aktivitet

    data class Avventende(val innspillTilArbeidsgiver: String, val fom: String, val tom: String) :
        Aktivitet

    data class Reisetilskudd(val fom: String, val tom: String) : Aktivitet
}

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

data class Hoveddiagnose(
    val system: DiagnoseSystem,
    val code: String,
)
