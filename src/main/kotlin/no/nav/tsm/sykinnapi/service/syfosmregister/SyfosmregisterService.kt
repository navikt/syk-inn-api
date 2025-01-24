package no.nav.tsm.sykinnapi.service.syfosmregister

import no.nav.tsm.sykinnapi.client.syfosmregister.SyfosmregisterClient
import no.nav.tsm.sykinnapi.modell.syfosmregister.SykmeldingDTO
import org.springframework.stereotype.Service

@Service
class SyfosmregisterService(
    private val syfosmregisterClient: SyfosmregisterClient,
) {

    fun getSykmelding(sykmeldingId: String): SykmeldingDTO {

        val sykmeldingDTO = syfosmregisterClient.getSykmelding(sykmeldingId)

        return sykmeldingDTO
    }
}
