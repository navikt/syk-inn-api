package no.nav.tsm.syk_inn_api.sykmelding

import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingResponse
import no.nav.tsm.syk_inn_api.sykmelding.response.toLightSykmelding
import no.nav.tsm.sykmelding.input.core.model.RuleType

fun sykmeldingAccessControl(
    hpr: String,
    sykmelding: SykmeldingDocument,
): SykmeldingResponse? {
    /** Behandlere are able to see the entire sykmelding if it's written by themselves. */
    if (sykmelding.meta.sykmelder.hprNummer == hpr) {
        return sykmelding
    }

    /**
     * "Light" sykmelding should only ever be OK, other behandlere are unable to see invalid
     * sykmeldinger.
     */
    if (sykmelding.utfall.result != RuleType.OK) return null

    /** Any other scenario, you only see a "Light" version. */
    return sykmelding.toLightSykmelding()
}
