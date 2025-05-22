package no.nav.tsm.syk_inn_api.model.sykmelding.kafka

import java.time.LocalDate

enum class Aktivitetstype {
    AKTIVITET_IKKE_MULIG,
    AVVENTENDE,
    BEHANDLINGSDAGER,
    GRADERT,
    REISETILSKUDD,
}

sealed interface AktivitetKafka {
    val fom: LocalDate
    val tom: LocalDate
    val type: Aktivitetstype
}

data class Behandlingsdager(
    val antallBehandlingsdager: Int,
    override val fom: LocalDate,
    override val tom: LocalDate
) : AktivitetKafka {
    override val type = Aktivitetstype.BEHANDLINGSDAGER
}

data class Gradert(
    val grad: Int,
    override val fom: LocalDate,
    override val tom: LocalDate,
    val reisetilskudd: Boolean,
) : AktivitetKafka {
    override val type = Aktivitetstype.GRADERT
}

data class Reisetilskudd(override val fom: LocalDate, override val tom: LocalDate) :
    AktivitetKafka {
    override val type = Aktivitetstype.REISETILSKUDD
}

data class Avventende(
    val innspillTilArbeidsgiver: String,
    override val fom: LocalDate,
    override val tom: LocalDate
) : AktivitetKafka {
    override val type = Aktivitetstype.AVVENTENDE
}

data class AktivitetIkkeMulig(
    val medisinskArsak: MedisinskArsak?,
    val arbeidsrelatertArsak: ArbeidsrelatertArsak?,
    override val fom: LocalDate,
    override val tom: LocalDate
) : AktivitetKafka {
    override val type = Aktivitetstype.AKTIVITET_IKKE_MULIG
}
