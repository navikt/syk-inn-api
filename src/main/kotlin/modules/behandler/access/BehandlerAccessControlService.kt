package no.nav.tsm.modules.behandler.access

import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding

class BehandlerAccessControlService {
    fun toRedactedIfNeeded(
        sykInnSykmelding: VerifiedSykInnSykmelding,
        currentBehandlerHpr: String,
    ): BehandlerSykmelding? {
        /** Behandlere are able to see the entire sykmelding if it's written by themselves. */
        if (sykInnSykmelding.meta.behandlerHpr == currentBehandlerHpr) {
            return sykInnSykmelding.toSykmelding()
        }

        /**
         * "Redacted" sykmelding should only ever be OK, other behandlere are unable to see invalid
         * sykmeldinger.
         */
        if (sykInnSykmelding.result !is SykInnSykmeldingRuleResult.OK) return null

        /** Any other scenario, you only see a "Redacted" version. */
        return sykInnSykmelding.toRedactedSykmelding()
    }
}
