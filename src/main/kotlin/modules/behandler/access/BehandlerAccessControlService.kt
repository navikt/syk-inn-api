package no.nav.tsm.modules.behandler.access

import no.nav.tsm.modules.behandler.payloads.BehandlerSykmelding
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding

class BehandlerAccessControlService {
    fun toRedactedIfNeeded(sykInnSykmelding: VerifiedSykInnSykmelding): BehandlerSykmelding {
        // TODO: Faktisk tilgangsstyring
        if (false) {
            sykInnSykmelding.toRedactedSykmelding()
        }

        return sykInnSykmelding.toSykmelding()
    }
}
