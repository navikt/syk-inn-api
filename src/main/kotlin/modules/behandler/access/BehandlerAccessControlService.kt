package modules.behandler.access

import modules.behandler.payloads.BehandlerSykmelding
import modules.sykmeldinger.domain.VerifiedSykInnSykmelding

class BehandlerAccessControlService {
    fun toRedactedIfNeeded(sykInnSykmelding: VerifiedSykInnSykmelding): BehandlerSykmelding {
        // TODO: Faktisk tilgangsstyring

        return sykInnSykmelding.toSykmelding()
    }
}
