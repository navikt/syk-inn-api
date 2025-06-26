package no.nav.tsm.syk_inn_api.sykmelder.hpr

import java.time.LocalDateTime
import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori

data class HprSykmelder(
    val godkjenninger: List<HprGodkjenning> = emptyList(),
    val fnr: String,
    val hprNummer: String?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

data class HprGodkjenning(
    val helsepersonellkategori: HprKode? = null,
    val autorisasjon: HprKode? = null,
    val tillegskompetanse: List<HprTilleggskompetanse>? = null,
)

data class HprTilleggskompetanse(
    val avsluttetStatus: HprKode?,
    val eTag: String?,
    val gyldig: HprGyldighetsPeriode?,
    val id: Int?,
    val type: HprKode?
)

data class HprGyldighetsPeriode(val fra: LocalDateTime?, val til: LocalDateTime?)

data class HprKode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?,
)

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
