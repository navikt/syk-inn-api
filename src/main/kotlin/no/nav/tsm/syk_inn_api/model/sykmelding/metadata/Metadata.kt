package no.nav.tsm.mottak.sykmelding.model.metadata

import java.time.OffsetDateTime

enum class MetadataType {
    DIGITAL
}

sealed interface MessageMetadata {
    val type: MetadataType
}

data class Digital(
    val orgnummer: String,
) : MessageMetadata {
    override val type: MetadataType = MetadataType.DIGITAL
}


enum class AckType {
    JA,
    NEI,
    KUN_VED_FEIL,
    IKKE_OPPGITT,
    UGYLDIG;

    companion object {
        fun parse(value: String?): AckType {
            return when (value) {
                null -> IKKE_OPPGITT
                "J" -> JA
                "N" -> NEI
                "F" -> KUN_VED_FEIL
                "" -> UGYLDIG
                else -> throw IllegalArgumentException("Unrecognized ack type: $value")
            }
        }
    }
}

data class Ack(
    val ackType: AckType,
)
