package no.nav.tsm.modules.sykmeldinger.rules.juridisk

class JuridiskHenvisningService(private val versjonsKode: String) {
    companion object {
        val EVENT_NAME = "subsumsjon"
        val VERSION = "1.0.0"
        val KILDE = "syk-inn-api"
    }
}
