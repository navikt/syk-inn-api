package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import no.nav.tsm.core.common.Navn
import no.nav.tsm.regulus.regula.payload.BehandlerGodkjenning
import no.nav.tsm.regulus.regula.payload.BehandlerKode
import no.nav.tsm.regulus.regula.payload.BehandlerPeriode
import no.nav.tsm.regulus.regula.payload.BehandlerTilleggskompetanse

data class SykmelderMedHpr(
    val ident: String,
    val navn: Navn,
    val hprNummer: String,
    val godkjenninger: List<SykmelderGodkjenning>,
)

/** Data type from regula. It's only used internally. Bad? */
typealias SykmelderGodkjenning = BehandlerGodkjenning

typealias SykmelderTilleggskompetanse = BehandlerTilleggskompetanse

typealias SykmelderPeriode = BehandlerPeriode

typealias SykmelderKodeverk = BehandlerKode
