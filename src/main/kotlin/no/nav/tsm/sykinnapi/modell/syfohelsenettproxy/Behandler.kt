package no.nav.tsm.sykinnapi.modell.syfohelsenettproxy

data class Behandler(
    val godkjenninger: List<Godkjenning>,
    val fnr: String?,
    val hprNummer: String?,
    val fornavn: String?,
    val mellomnavn: String?,
    val etternavn: String?,
)

data class Godkjenning(
    val helsepersonellkategori: Kode? = null,
    val autorisasjon: Kode? = null,
)

data class Kode(
    val aktiv: Boolean,
    val oid: Int,
    val verdi: String?,
)
