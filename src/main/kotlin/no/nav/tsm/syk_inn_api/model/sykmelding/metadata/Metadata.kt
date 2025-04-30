package no.nav.tsm.mottak.sykmelding.model.metadata

import java.time.OffsetDateTime

enum class MetadataType {
    ENKEL,
    EMOTTAK,
    UTENLANDSK_SYKMELDING,
    PAPIRSYKMELDING,
    EGENMELDT,
}

sealed interface Meldingsinformasjon {
    val type: MetadataType
    val vedlegg: List<String>?
}

data class Egenmeldt(
    val msgInfo: MeldingMetadata,
) : Meldingsinformasjon {
    override val type: MetadataType = MetadataType.EGENMELDT
    override val vedlegg: List<String> = emptyList()
}

data class Papirsykmelding(
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val journalPostId: String,
) : Meldingsinformasjon {
    override val vedlegg = null
    override val type = MetadataType.PAPIRSYKMELDING
}

data class Utenlandsk(
    val land: String,
    val journalPostId: String,
) : Meldingsinformasjon {
    override val vedlegg = null
    override val type: MetadataType = MetadataType.UTENLANDSK_SYKMELDING
}

data class EmottakEnkel(
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.ENKEL
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
    val msgInfo: MeldingMetadata,
    val sender: Organisasjon,
    val receiver: Organisasjon,
    val pasient: Pasient?,
    override val vedlegg: List<String>?,
) : Meldingsinformasjon {
    override val type = MetadataType.EMOTTAK
}

enum class Meldingstype {
    SYKMELDING;

    companion object {
        fun parse(v: String): Meldingstype = when (v) {
            "SYKMELD" -> SYKMELDING
            else -> throw IllegalArgumentException("Ukjent meldingstype: $v")
        }
    }
}


data class MeldingMetadata(
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
