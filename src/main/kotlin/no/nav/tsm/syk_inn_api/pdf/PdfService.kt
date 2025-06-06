package no.nav.tsm.syk_inn_api.pdf

import java.util.*
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingService
import org.springframework.stereotype.Service

@Service
class PdfService(
    private val sykmeldingService: SykmeldingService,
    private val personService: PersonService,
    private val pdfGenerator: PdfGenerator
) {
    fun createSykmeldingPdf(sykmeldingId: UUID, hpr: String): Result<ByteArray?> {
        val sykmelding =
            sykmeldingService.getSykmeldingById(sykmeldingId, hpr) ?: return Result.success(null)

        return personService
            .getPersonByIdent(sykmelding.pasientFnr)
            .map { pasient -> buildSykmeldingHtml(sykmelding, pasient) }
            .map { html ->
                val pdf = pdfGenerator.generatePDFA(html)

                pdfGenerator.verifyPDFACompliance(pdf)

                pdf
            }
    }
}
