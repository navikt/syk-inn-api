package modules.behandler.payloads

import no.nav.tsm.regulus.regula.RegulaOutcomeStatus

data class BehandlerSykmeldingVerify(
    val status: RegulaOutcomeStatus,
    val message: String,
    val rule: String,
)
