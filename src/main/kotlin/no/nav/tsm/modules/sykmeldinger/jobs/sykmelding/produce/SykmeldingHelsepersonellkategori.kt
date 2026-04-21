@file:Suppress("detekt.MagicNumber")

package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori

fun helsepersonellkategoriPresedence(kategori: HelsepersonellKategori) =
    when (kategori) {
        HelsepersonellKategori.LEGE -> 1
        HelsepersonellKategori.TANNLEGE -> 2
        HelsepersonellKategori.PSYKOLOG -> 3
        HelsepersonellKategori.MANUELLTERAPEUT -> 4
        HelsepersonellKategori.FYSIOTERAPEUT -> 5
        HelsepersonellKategori.KIROPRAKTOR -> 6
        HelsepersonellKategori.SYKEPLEIER -> 7
        HelsepersonellKategori.NAPRAPAT -> 7
        HelsepersonellKategori.HJELPEPLEIER -> 7
        HelsepersonellKategori.HELSESEKRETAR -> 7
        HelsepersonellKategori.HELSEFAGARBEIDER -> 7
        HelsepersonellKategori.AUDIOGRAF -> 7
        HelsepersonellKategori.AMBULANSEARBEIDER -> 7
        HelsepersonellKategori.FOTTERAPEUT -> 7
        HelsepersonellKategori.JORDMOR -> 7
        HelsepersonellKategori.TANNHELSESEKRETAR -> 7
        HelsepersonellKategori.USPESIFISERT -> 7
        HelsepersonellKategori.UGYLDIG -> 7
        HelsepersonellKategori.IKKE_OPPGITT -> 7
    }

fun parseHelsepersonellKategori(v: String?): HelsepersonellKategori {
    return when (v) {
        "HE" -> HelsepersonellKategori.HELSESEKRETAR
        "KI" -> HelsepersonellKategori.KIROPRAKTOR
        "LE" -> HelsepersonellKategori.LEGE
        "MT" -> HelsepersonellKategori.MANUELLTERAPEUT
        "TL" -> HelsepersonellKategori.TANNLEGE
        "TH" -> HelsepersonellKategori.TANNHELSESEKRETAR
        "FT" -> HelsepersonellKategori.FYSIOTERAPEUT
        "SP" -> HelsepersonellKategori.SYKEPLEIER
        "HP" -> HelsepersonellKategori.HJELPEPLEIER
        "HF" -> HelsepersonellKategori.HELSEFAGARBEIDER
        "JO" -> HelsepersonellKategori.JORDMOR
        "AU" -> HelsepersonellKategori.AUDIOGRAF
        "NP" -> HelsepersonellKategori.NAPRAPAT
        "PS" -> HelsepersonellKategori.PSYKOLOG
        "FO" -> HelsepersonellKategori.FOTTERAPEUT
        "AA" -> HelsepersonellKategori.AMBULANSEARBEIDER
        "XX" -> HelsepersonellKategori.USPESIFISERT
        "HS" -> HelsepersonellKategori.UGYLDIG
        "token" -> HelsepersonellKategori.UGYLDIG
        null -> HelsepersonellKategori.IKKE_OPPGITT
        else -> throw IllegalArgumentException("Ukjent helsepersonellkategori: $v")
    }
}
