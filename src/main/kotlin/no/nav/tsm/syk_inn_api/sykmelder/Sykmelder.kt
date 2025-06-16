package no.nav.tsm.syk_inn_api.sykmelder

import no.nav.tsm.syk_inn_api.common.Navn
import no.nav.tsm.syk_inn_api.sykmelder.hpr.HprGodkjenning

sealed interface Sykmelder {
    val hpr: String
    val ident: String
    val navn: Navn?

    /*
        We'll allow this object to bleed through, but should possibly not since it is the API
        response from helsenettproxy response.
    */
    val godkjenninger: List<HprGodkjenning>

    data class Enkel(
        override val hpr: String,
        override val navn: Navn?,
        override val ident: String,
        override val godkjenninger: List<HprGodkjenning>,
    ) : Sykmelder {
        fun toMedSuspensjon(suspendert: Boolean): MedSuspensjon {
            return MedSuspensjon(
                hpr = this.hpr,
                navn = this.navn,
                ident = this.ident,
                godkjenninger = this.godkjenninger,
                suspendert = suspendert,
            )
        }
    }

    data class MedSuspensjon(
        override val hpr: String,
        override val navn: Navn?,
        override val ident: String,
        override val godkjenninger: List<HprGodkjenning>,
        val suspendert: Boolean
    ) : Sykmelder
}
