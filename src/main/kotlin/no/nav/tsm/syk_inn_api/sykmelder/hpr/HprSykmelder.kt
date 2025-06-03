package no.nav.tsm.syk_inn_api.sykmelder.hpr

import java.time.LocalDateTime

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
