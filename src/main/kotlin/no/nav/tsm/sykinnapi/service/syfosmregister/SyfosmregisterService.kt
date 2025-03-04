package no.nav.tsm.sykinnapi.service.syfosmregister

import no.nav.tsm.sykinnapi.client.syfosmregister.SyfosmregisterClient
import no.nav.tsm.sykinnapi.modell.syfosmregister.SyfoSmRegisterSykmelding
import org.springframework.stereotype.Service

@Service
class SyfosmregisterService(
    private val syfosmregisterClient: SyfosmregisterClient,
) {
    fun getSykmelding(sykmeldingId: String): SyfoSmRegisterSykmelding =
        syfosmregisterClient.getSykmelding(sykmeldingId)

    fun getSykmeldingByIdent(ident: String): List<SyfoSmRegisterSykmelding> =
        syfosmregisterClient.getSykmeldingByIdent(ident)
}
