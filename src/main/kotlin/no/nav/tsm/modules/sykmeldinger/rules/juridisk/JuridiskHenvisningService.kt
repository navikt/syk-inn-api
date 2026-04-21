package no.nav.tsm.modules.sykmeldinger.rules.juridisk

class JuridiskHenvisningService(private val versjonsKode: String) {
    companion object {
        const val EVENT_NAME = "subsumsjon"
        const val VERSION = "1.0.0"
        const val KILDE = "syk-inn-api"
    }
}
