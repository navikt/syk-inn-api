package no.nav.tsm.modules.sykmeldinger.sykmelder

import no.nav.tsm.core.common.Navn
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.SykmelderGodkjenning

sealed interface Sykmelder {
    data class MedSuspensjon(
        val hpr: String,
        val navn: Navn,
        val godkjenninger: List<SykmelderGodkjenning>,
        val ident: String,
        val suspendert: Boolean,
    ) : Sykmelder

    object FinnesIkke : Sykmelder
}
