package no.nav.tsm.syk_inn_api.pdf

import java.util.*
import no.nav.tsm.syk_inn_api.model.SykmeldingResult
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.service.SykmeldingService
import org.springframework.stereotype.Service

@Service
class PdfService(
    private val sykmeldingService: SykmeldingService,
    private val personService: PersonService
) {
    fun createSykmeldingPdf(sykmeldingId: UUID, hpr: String): Result<ByteArray> {
        val sykmelding =
            // TODO: Clean this up
            when (val result = sykmeldingService.getSykmeldingById(sykmeldingId, hpr)) {
                is SykmeldingResult.Failure ->
                    return Result.failure(IllegalStateException(result.errorMessage))
                is SykmeldingResult.Success -> result.sykmeldingResponse
                        ?: throw IllegalStateException("Got success, but no savedSykmelding")
            }

        val pasient = personService.getPersonByIdent(sykmelding.pasientFnr)
        val html = buildSykmeldingHtml(sykmelding, pasient)

        return Result.success(createPDFA(html))
    }
}
