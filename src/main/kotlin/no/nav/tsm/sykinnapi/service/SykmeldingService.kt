package no.nav.tsm.sykinnapi.service

import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import org.springframework.stereotype.Service

@Service
class SykmeldingService {
    fun create(sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload): Boolean {
        // TODO map the sykInnApiNySykmeldingPayload to revicedsykmelding model, put on kafka topic
        return true
    }
}
