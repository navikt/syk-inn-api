package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.model.SykmeldingResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class SykmeldingPdfService {
    fun getPdf(sykmeldingId: String, hpr: String): SykmeldingResult {

        // TODO implement - kall dedikert app som genererer pdf
        return SykmeldingResult.Success(
            statusCode = HttpStatus.CREATED, // Created eller ok? vi genererer jo pdfen ved behov
            pdf = "pdf".toByteArray()
        )
    }
}
