package modules.sykmeldinger.rules.juridisk

import no.nav.syfo.rules.juridiskvurdering.*
import no.nav.tsm.regulus.regula.*

class JuridiskHenvisningService(private val versjonsKode: String) {
    companion object {
        val EVENT_NAME = "subsumsjon"
        val VERSION = "1.0.0"
        val KILDE = "syk-inn-api"
    }
}
