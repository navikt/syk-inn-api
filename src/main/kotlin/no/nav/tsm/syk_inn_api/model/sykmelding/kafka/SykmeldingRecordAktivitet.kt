package no.nav.tsm.syk_inn_api.model.sykmelding.kafka

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import java.time.LocalDate

enum class SykmeldingRecordAktivitetsType {
    AKTIVITET_IKKE_MULIG,
    AVVENTENDE,
    BEHANDLINGSDAGER,
    GRADERT,
    REISETILSKUDD,
}

@JsonSubTypes(
    JsonSubTypes.Type(
        SykmeldingRecordAktivitet.AktivitetIkkeMulig::class,
        name = "AKTIVITET_IKKE_MULIG"
    ),
    JsonSubTypes.Type(SykmeldingRecordAktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(SykmeldingRecordAktivitet.Avventende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(SykmeldingRecordAktivitet.Behandlingsdager::class, name = "BEHANDLINGSDAGER"),
    JsonSubTypes.Type(SykmeldingRecordAktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface SykmeldingRecordAktivitet {
    val fom: LocalDate
    val tom: LocalDate
    val type: SykmeldingRecordAktivitetsType

    data class Behandlingsdager(
        val antallBehandlingsdager: Int,
        override val fom: LocalDate,
        override val tom: LocalDate
    ) : SykmeldingRecordAktivitet {
        override val type = SykmeldingRecordAktivitetsType.BEHANDLINGSDAGER
    }

    data class Gradert(
        val grad: Int,
        override val fom: LocalDate,
        override val tom: LocalDate,
        val reisetilskudd: Boolean,
    ) : SykmeldingRecordAktivitet {
        override val type = SykmeldingRecordAktivitetsType.GRADERT
    }

    data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
        SykmeldingRecordAktivitet {
        override val type = SykmeldingRecordAktivitetsType.REISETILSKUDD
    }

    data class Avventende(
        val innspillTilArbeidsgiver: String,
        override val fom: LocalDate,
        override val tom: LocalDate
    ) : SykmeldingRecordAktivitet {
        override val type = SykmeldingRecordAktivitetsType.AVVENTENDE
    }

    data class AktivitetIkkeMulig(
        val medisinskArsak: MedisinskArsak?,
        val arbeidsrelatertArsak: ArbeidsrelatertArsak?,
        override val fom: LocalDate,
        override val tom: LocalDate
    ) : SykmeldingRecordAktivitet {
        override val type = SykmeldingRecordAktivitetsType.AKTIVITET_IKKE_MULIG
    }
}
