package no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata

import java.time.OffsetDateTime
import no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding.Pasient

enum class MetadataType {
    DIGITAL,
    ENKEL,
    EMOTTAK,
    PAPIRSYKMELDING,
    EGENMELDT,
    UTENLANDSK_SYKMELDING,
}

sealed interface MessageMetadata {
    val type: MetadataType
}

data class Digital(
    val orgnummer: String,
) : MessageMetadata {
    override val type: MetadataType = MetadataType.DIGITAL
}

data class Papir(
    val msgInfo: MessageInfo,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val journalPostId: String,
) : MessageMetadata {
    override val type = MetadataType.PAPIRSYKMELDING
}

data class EmottakEnkel(
    val msgInfo: MessageInfo,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val vedlegg: List<String>?,
) : MessageMetadata {
    override val type = MetadataType.ENKEL
}

data class Egenmeldt(
    val msgInfo: MessageInfo,
) : MessageMetadata {
    override val type: MetadataType = MetadataType.EGENMELDT
}

data class Utenlandsk(
    val land: String,
    val journalPostId: String,
) : MessageMetadata {
    override val type: MetadataType = MetadataType.UTENLANDSK_SYKMELDING
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

data class EDIEmottak(
    val mottakenhetBlokk: MottakenhetBlokk,
    val ack: Ack,
    val msgInfo: MessageInfo,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val pasient: Pasient?,
    val vedlegg: List<String>?,
) : MessageMetadata {
    override val type = MetadataType.EMOTTAK
}

enum class Meldingstype {
    SYKMELDING;

    companion object {
        fun parse(v: String): Meldingstype =
            when (v) {
                "SYKMELD" -> SYKMELDING
                else -> throw IllegalArgumentException("Ukjent meldingstype: $v")
            }
    }
}

data class MessageInfo(
    val type: Meldingstype,
    val genDate: OffsetDateTime,
    val msgId: String,
    val migVersjon: String?,
)

data class MottakenhetBlokk(
    val ediLogid: String,
    val avsender: String,
    val ebXMLSamtaleId: String,
    val mottaksId: String?,
    val meldingsType: String,
    val avsenderRef: String,
    val avsenderFnrFraDigSignatur: String?,
    val mottattDato: OffsetDateTime,
    val orgnummer: String?,
    val avsenderOrgNrFraDigSignatur: String?,
    val partnerReferanse: String,
    val herIdentifikator: String,
    val ebRole: String,
    val ebService: String,
    val ebAction: String,
)
