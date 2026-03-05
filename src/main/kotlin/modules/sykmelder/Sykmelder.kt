package modules.sykmelder

sealed interface Sykmelder {
    val hpr: String
    val ident: String

    data class Enkel(override val hpr: String, override val ident: String) : Sykmelder {
        fun toMedSuspensjon(suspendert: Boolean): MedSuspensjon {
            return MedSuspensjon(hpr = this.hpr, ident = this.ident, suspendert = suspendert)
        }
    }

    data class MedSuspensjon(
        override val hpr: String,
        override val ident: String,
        val suspendert: Boolean,
    ) : Sykmelder
}
