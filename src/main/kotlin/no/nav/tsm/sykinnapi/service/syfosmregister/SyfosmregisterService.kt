package no.nav.tsm.sykinnapi.service.syfosmregister

import no.nav.tsm.sykinnapi.client.syfosmregister.SyfosmregisterClient
import no.nav.tsm.sykinnapi.modell.syfosmregister.SykInnSykmeldingDTO
import org.springframework.stereotype.Service

@Service
class SyfosmregisterService(
    private val syfosmregisterClient: SyfosmregisterClient,
) {

    fun getSykmelding(sykmeldingId: String): SykInnSykmeldingDTO {

        val sykInnSykmeldingDTO = syfosmregisterClient.getSykmelding(sykmeldingId)

        return sykInnSykmeldingDTO
    }

    fun getSykmeldingByIdent(ident: String): List<SykInnSykmeldingDTO> {

        val sykInnSykmeldingerDTO = syfosmregisterClient.getSykmeldingByIdent(ident)

        return sykInnSykmeldingerDTO
    }
}
