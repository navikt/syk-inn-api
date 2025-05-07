package no.nav.tsm.syk_inn_api.model.sykmelding.kafka

import java.time.LocalDate

enum class ARBEIDSGIVER_TYPE {
    EN_ARBEIDSGIVER,
    FLERE_ARBEIDSGIVERE,
    INGEN_ARBEIDSGIVER,
}

sealed interface ArbeidsgiverInfo {
    val type: ARBEIDSGIVER_TYPE
}

data class EnArbeidsgiver(
    val meldingTilArbeidsgiver: String?,
    val tiltakArbeidsplassen: String?,
) : ArbeidsgiverInfo {
    override val type: ARBEIDSGIVER_TYPE = ARBEIDSGIVER_TYPE.EN_ARBEIDSGIVER
}

data class FlereArbeidsgivere(
    val navn: String?,
    val yrkesbetegnelse: String?,
    val stillingsprosent: Int?,
    val meldingTilArbeidsgiver: String?,
    val tiltakArbeidsplassen: String?
) : ArbeidsgiverInfo {
    override val type: ARBEIDSGIVER_TYPE = ARBEIDSGIVER_TYPE.FLERE_ARBEIDSGIVERE
}

class IngenArbeidsgiver() : ArbeidsgiverInfo {
    override val type: ARBEIDSGIVER_TYPE = ARBEIDSGIVER_TYPE.INGEN_ARBEIDSGIVER

    override fun equals(other: Any?) = other is IngenArbeidsgiver

    override fun hashCode() = type.hashCode()

    override fun toString() = "IngenArbeidsgiver(type=$type)"
}

enum class IArbeidType {
    ER_I_ARBEID,
    ER_IKKE_I_ARBEID,
}

sealed interface IArbeid {
    val type: IArbeidType
    val vurderingsdato: LocalDate?
}

data class ErIArbeid(
    val egetArbeidPaSikt: Boolean,
    val annetArbeidPaSikt: Boolean,
    val arbeidFOM: LocalDate?,
    override val vurderingsdato: LocalDate?,
) : IArbeid {
    override val type = IArbeidType.ER_I_ARBEID
}

data class ErIkkeIArbeid(
    val arbeidsforPaSikt: Boolean,
    val arbeidsforFOM: LocalDate?,
    override val vurderingsdato: LocalDate?,
) : IArbeid {
    override val type = IArbeidType.ER_IKKE_I_ARBEID
}
