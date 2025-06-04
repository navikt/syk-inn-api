package no.nav.tsm.syk_inn_api.sykmelding.kafka.sykmelding

import java.time.LocalDate

enum class KafkaDiagnoseSystem {
    ICPC2,
    ICD10,
    ICPC2B,
    PHBU,
    UGYLDIG
}

data class KafkaDiagnoseInfo(
    val system: KafkaDiagnoseSystem,
    val kode: String,
)

enum class KafkaMedisinskArsakType {
    TILSTAND_HINDRER_AKTIVITET,
    AKTIVITET_FORVERRER_TILSTAND,
    AKTIVITET_FORHINDRER_BEDRING,
    ANNET,
}

enum class KafkaArbeidsrelatertArsakType {
    MANGLENDE_TILRETTELEGGING,
    ANNET,
}

enum class KafkaAnnenFravarArsakType {
    GODKJENT_HELSEINSTITUSJON,
    BEHANDLING_FORHINDRER_ARBEID,
    ARBEIDSRETTET_TILTAK,
    MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND,
    NODVENDIG_KONTROLLUNDENRSOKELSE,
    SMITTEFARE,
    ABORT,
    UFOR_GRUNNET_BARNLOSHET,
    DONOR,
    BEHANDLING_STERILISERING,
}

data class KafkaAnnenFraverArsak(
    val beskrivelse: String?,
    val arsak: List<KafkaAnnenFravarArsakType>?
)

data class KafkaMedisinskArsak(val beskrivelse: String?, val arsak: KafkaMedisinskArsakType)

data class KafkaArbeidsrelatertArsak(
    val beskrivelse: String?,
    val arsak: KafkaArbeidsrelatertArsakType
)

data class KafkaYrkesskade(val yrkesskadeDato: LocalDate?)

data class SykmeldingRecordMedisinskVurdering(
    val hovedDiagnose: KafkaDiagnoseInfo?,
    val biDiagnoser: List<KafkaDiagnoseInfo>?,
    val svangerskap: Boolean, // TODO må få inn i payload
    val yrkesskade: KafkaYrkesskade?,
    val skjermetForPasient: Boolean, // TODO må få inn i payload
    val syketilfelletStartDato: LocalDate?,
    val annenFraversArsak: KafkaAnnenFraverArsak?,
)
