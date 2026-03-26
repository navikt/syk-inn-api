package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

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
    val helsepersonellkategori: HprKodeverk? = null,
    val autorisasjon: HprKodeverk? = null,
    val tillegskompetanse: List<HprTilleggskompetanse>? = null,
)

data class HprTilleggskompetanse(
    val type: HprKodeverk?,
    val avsluttetStatus: HprKodeverk?,
    val gyldig: HprGyldighetsPeriode?,
)

data class HprGyldighetsPeriode(val fra: LocalDateTime?, val til: LocalDateTime?)

data class HprKodeverk(val aktiv: Boolean, val oid: Int, val verdi: String?)
