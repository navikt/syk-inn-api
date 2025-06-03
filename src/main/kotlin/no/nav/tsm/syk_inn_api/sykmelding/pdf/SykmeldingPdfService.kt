package no.nav.tsm.syk_inn_api.sykmelding.pdf

import no.nav.tsm.syk_inn_api.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingResult
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service

@Service
class SykmeldingPdfService(
    private val sykmeldingPersistenceService: SykmeldingPersistenceService,
) {
    fun getPdf(sykmeldingId: String, hpr: String): SykmeldingResult {
        val sykmelding = sykmeldingPersistenceService.getSykmeldingById(sykmeldingId)
        // TODO implement - kall dedikert app som genererer pdf -innholdet skal være det legen har
        // sendt inn til oss når dei sendte sykmeldinga tidligare.
        return SykmeldingResult.Success(
            statusCode = HttpStatus.CREATED, // Created eller ok? vi genererer jo pdfen ved behov
            pdf = "pdf".toByteArray()
        )
    }
}
