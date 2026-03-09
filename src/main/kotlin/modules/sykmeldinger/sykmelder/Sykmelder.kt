package modules.sykmeldinger.sykmelder

sealed interface Sykmelder {
    val hpr: String

    data class MedSuspensjon(override val hpr: String, val ident: String, val suspendert: Boolean) :
        Sykmelder

    data class UtenSuspensjon(override val hpr: String, val ident: String) : Sykmelder

    data class FinnesIkke(override val hpr: String) : Sykmelder
}
