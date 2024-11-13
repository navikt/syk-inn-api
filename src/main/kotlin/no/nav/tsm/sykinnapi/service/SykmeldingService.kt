package no.nav.tsm.sykinnapi.service

import no.nav.tsm.sykinnapi.mapper.receivedSykmeldingWithValidationMapper
import no.nav.tsm.sykinnapi.modell.SykInnApiNySykmeldingPayload
import org.springframework.stereotype.Service

@Service
class SykmeldingService {
    fun create(sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload): String {
        // TODO put receivedSykmeldingWithValidationResult on kafka topic
        // TODO Do rule check here, api? regilus-maximus, or call to syfosmregler?
        // TODO hente legens fnr, fra syfohelsenettproxy, eller direkte fra HPR registeret?

        val receivedSykmeldingWithValidation =
            receivedSykmeldingWithValidationMapper(sykInnApiNySykmeldingPayload)

        return receivedSykmeldingWithValidation.sykmelding.id
    }
}
