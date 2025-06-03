package no.nav.tsm.syk_inn_api.sykmelder

import java.time.LocalDateTime

data class Sykmelder(
    val godkjenninger: List<Godkjenning> = emptyList(),
    val fnr: String,
    val hprNummer: String?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

data class Godkjenning(
    val helsepersonellkategori: Kode? = null,
    val autorisasjon: Kode? = null,
    val tillegskompetanse: List<Tilleggskompetanse>? = null,
)

data class Tilleggskompetanse(
    val avsluttetStatus: Kode?,
    val eTag: String?,
    val gyldig: GyldighetsPeriode?,
    val id: Int?,
    val type: Kode?
)

data class GyldighetsPeriode(val fra: LocalDateTime?, val til: LocalDateTime?)

data class Kode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?,
)
