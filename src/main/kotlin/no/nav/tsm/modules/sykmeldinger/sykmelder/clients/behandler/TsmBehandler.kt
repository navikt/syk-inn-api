package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler

import java.time.LocalDate

data class TsmBehandler(
    val hpr: String,
    val ident: String,
    val navn: BehandlerNavn?,
    val godkjenning: List<TsmGodkjenning>,
    val suspendert: Boolean,
)

data class BehandlerNavn(val fornavn: String, val mellomnavn: String?, val etternavn: String)

data class TsmGodkjenning(
    val autorisasjon: TsmBehandlerKode,
    val helsepersonellkategori: TsmBehandlerKode,
    val tilleggskompetanse: List<TsmBehandlerTilleggskompetanse>,
)

data class TsmBehandlerTilleggskompetanse(
    val avsluttetStatus: TsmBehandlerKode,
    val gyldig: TsmPeriode,
    val type: TsmBehandlerKode,
)

data class TsmBehandlerKode(val oid: Int, val verdi: String)

data class TsmPeriode(val fra: LocalDate, val til: LocalDate?)
