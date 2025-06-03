package no.nav.tsm.syk_inn_api.sykmelding.kafka.metadata

import java.time.LocalDate

enum class PersonIdType {
    FNR,
    DNR,
    HNR,
    HPR,
    HER,
    PNR,
    SEF,
    DKF,
    SSN,
    FPN,
    XXX,
    DUF,
    IKKE_OPPGITT,
    UGYLDIG;

    companion object {
        fun parse(type: String?): PersonIdType {
            return when (type) {
                "FNR" -> FNR
                "DNR" -> DNR
                "HNR" -> HNR
                "HPR" -> HPR
                "HER" -> HER
                "PNR" -> PNR
                "SEF" -> SEF
                "DKF" -> DKF
                "SSN" -> SSN
                "FPN" -> FPN
                "DUF" -> DUF
                "XXX" -> XXX
                "" -> UGYLDIG
                null -> return IKKE_OPPGITT
                else -> throw IllegalArgumentException("PersonIdType $type not supported")
            }
        }
    }
}

enum class Kjonn {
    MANN,
    KVINNE,
    USPESIFISERT,
    IKKE_OPPGITT,
    UGYLDIG;

    companion object {
        fun parse(v: String?): Kjonn {
            return when (v) {
                "1" -> MANN
                "2" -> KVINNE
                "9" -> USPESIFISERT
                "K" -> UGYLDIG
                "M" -> UGYLDIG
                "0" -> UGYLDIG
                "U" -> UGYLDIG
                null -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent kjønn: $v")
            }
        }
    }
}

data class Navn(val fornavn: String, val mellomnavn: String?, val etternavn: String)

data class PersonId(
    val id: String,
    val type: PersonIdType,
)

enum class HelsepersonellKategori {
    HELSESEKRETAR,
    KIROPRAKTOR,
    LEGE,
    MANUELLTERAPEUT,
    TANNLEGE,
    FYSIOTERAPEUT,
    SYKEPLEIER,
    HJELPEPLEIER,
    HELSEFAGARBEIDER,
    USPESIFISERT,
    JORDMOR,
    AUDIOGRAF,
    NAPRAPAT,
    AMBULANSEARBEIDER,
    PSYKOLOG,
    FOTTERAPEUT,
    TANNHELSESEKRETAR,
    UGYLDIG,
    IKKE_OPPGITT;

    companion object {
        fun parse(v: String?): HelsepersonellKategori {
            return when (v) {
                "HE" -> HELSESEKRETAR
                "KI" -> KIROPRAKTOR
                "LE" -> LEGE
                "MT" -> MANUELLTERAPEUT
                "TL" -> TANNLEGE
                "TH" -> TANNHELSESEKRETAR
                "FT" -> FYSIOTERAPEUT
                "SP" -> SYKEPLEIER
                "HP" -> HJELPEPLEIER
                "HF" -> HELSEFAGARBEIDER
                "JO" -> JORDMOR
                "AU" -> AUDIOGRAF
                "NP" -> NAPRAPAT
                "PS" -> PSYKOLOG
                "FO" -> FOTTERAPEUT
                "AA" -> AMBULANSEARBEIDER
                "XX" -> USPESIFISERT
                "HS" -> UGYLDIG
                "token" -> UGYLDIG
                null -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent helsepersonellkategori: $v")
            }
        }
    }
}

enum class RolleTilPasient {
    JOURNALANSVARLIG,
    FASTLEGE,
    IKKE_OPPGITT;

    companion object {
        fun parse(v: String?): RolleTilPasient {
            return when (v) {
                "4" -> JOURNALANSVARLIG
                "6" -> FASTLEGE
                null -> IKKE_OPPGITT
                else -> throw IllegalArgumentException("Ukjent rolle til pasient: $v")
            }
        }
    }
}

data class Helsepersonell(
    val ids: List<PersonId>,
    val navn: Navn?,
    val fodselsdato: LocalDate?,
    val kjonn: Kjonn?,
    val nasjonalitet: String?,
    val adresse: Adresse?,
    val kontaktinfo: List<Kontaktinfo>,
    val helsepersonellKategori: HelsepersonellKategori,
    val rolleTilPasient: RolleTilPasient,
)
