package no.nav.tsm.syk_inn_api.model.sykmelding.kafka

import java.time.LocalDate

enum class DiagnoseSystem {
    ICPC2, ICD10, ICPC2B, PHBU, UGYLDIG
}

data class DiagnoseInfo(
    val system: DiagnoseSystem,
    val kode: String,
)

enum class MedisinskArsakType {
    TILSTAND_HINDRER_AKTIVITET, AKTIVITET_FORVERRER_TILSTAND, AKTIVITET_FORHINDRER_BEDRING, ANNET,
}

enum class ArbeidsrelatertArsakType {
    MANGLENDE_TILRETTELEGGING, ANNET,
}

enum class AnnenFravarArsakType {
    GODKJENT_HELSEINSTITUSJON, BEHANDLING_FORHINDRER_ARBEID, ARBEIDSRETTET_TILTAK, MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND, NODVENDIG_KONTROLLUNDENRSOKELSE, SMITTEFARE, ABORT, UFOR_GRUNNET_BARNLOSHET, DONOR, BEHANDLING_STERILISERING,
}

data class AnnenFraverArsak(
    val beskrivelse: String?, val arsak: List<AnnenFravarArsakType>?
)

data class MedisinskArsak(
    val beskrivelse: String?, val arsak: MedisinskArsakType
)

data class ArbeidsrelatertArsak(
    val beskrivelse: String?, val arsak: ArbeidsrelatertArsakType
)

data class Yrkesskade(
    val yrkesskadeDato: LocalDate?
)

data class MedisinskVurdering(
    val hovedDiagnose: DiagnoseInfo?,
    val biDiagnoser: List<DiagnoseInfo>?,
    val svangerskap: Boolean,
    val yrkesskade: Yrkesskade?,
    val skjermetForPasient: Boolean,
    val syketilfelletStartDato: LocalDate?,
    val annenFraversArsak: AnnenFraverArsak?,
)
