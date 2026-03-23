package no.nav.tsm.modules.sykmeldinger.sykmelder

import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.SykmelderGodkjenning

sealed interface Sykmelder {
    val hpr: String
    val godkjenninger: List<SykmelderGodkjenning>

    data class MedSuspensjon(
        override val hpr: String,
        override val godkjenninger: List<SykmelderGodkjenning>,
        val ident: String,
        val suspendert: Boolean,
    ) : Sykmelder

    data class UtenSuspensjon(
        override val hpr: String,
        override val godkjenninger: List<SykmelderGodkjenning>,
        val ident: String,
    ) : Sykmelder

    data class FinnesIkke(
        override val hpr: String,
        override val godkjenninger: List<SykmelderGodkjenning>,
    ) : Sykmelder
}
