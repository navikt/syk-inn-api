package no.nav.tsm.modules.behandler.access

import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding

class BehandlerAccessControlService {
    fun toRedactedIfNeeded(
        sykInnSykmelding: VerifiedSykInnSykmelding,
        currentBehandlerHpr: String,
    ): BehandlerSykmelding? {
        val sykmeldingBehandlerHpr =
            when (sykInnSykmelding.meta) {
                is SykInnSykmeldingMeta.Digital -> sykInnSykmelding.meta.behandler.hpr
                is SykInnSykmeldingMeta.Legacy -> sykInnSykmelding.meta.behandler.hpr
                is SykInnSykmeldingMeta.Utenlandsk -> null
            }

        /** Behandlere are able to see the entire sykmelding if it's written by themselves. */
        if (sykmeldingBehandlerHpr == currentBehandlerHpr) {
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
