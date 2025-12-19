package no.nav.tsm.syk_inn_api.sykmelding

import no.nav.tsm.syk_inn_api.sykmelding.persistence.PersistedRuleType
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingResponse
import no.nav.tsm.syk_inn_api.sykmelding.response.toRedactedSykmelding

/** Any sykmelding returned to user should always be access controlled through this */
fun sykmeldingAccessControl(
    hpr: String,
    sykmelding: SykmeldingDocument,
): SykmeldingResponse? {
    /** Behandlere are able to see the entire sykmelding if it's written by themselves. */
    if (sykmelding.meta.sykmelder.hprNummer == hpr) {
        return sykmelding
    }

    /**
     * "Redacted" sykmelding should only ever be OK, other behandlere are unable to see invalid
     * sykmeldinger.
     */
    if (sykmelding.utfall.result != PersistedRuleType.OK) return null

    /** Any other scenario, you only see a "Redacted" version. */
    return sykmelding.toRedactedSykmelding()
}
