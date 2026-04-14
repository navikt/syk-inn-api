package no.nav.tsm.modules.behandler.payloads

import no.nav.tsm.sykmelding.input.core.model.RuleType

data class BehandlerSykmeldingVerify(val status: RuleType, val message: String?, val rule: String?)
