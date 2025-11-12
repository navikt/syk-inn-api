package no.nav.tsm.syk_inn_api.pdf

import io.opentelemetry.instrumentation.annotations.WithSpan
import java.util.*
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingService
import no.nav.tsm.syk_inn_api.utils.failSpan
import org.springframework.stereotype.Service

@Service
class PdfService(
    private val sykmeldingService: SykmeldingService,
    private val personService: PersonService,
    private val pdfGenerator: PdfGenerator
) {
    @WithSpan
    fun createSykmeldingPdf(sykmeldingId: UUID, hpr: String): Result<ByteArray?> {
        val sykmelding =
            sykmeldingService.getSykmeldingById(sykmeldingId) ?: return Result.success(null)

        if (sykmelding.meta.sykmelder.hprNummer != hpr) {
            return Result.failure(
                failSpan(
                    IllegalStateException(
                        "Sykmelding $sykmeldingId does not belong to HPR-number $hpr",
                    ),
                ),
            )
        }

        return personService
            .getPersonByIdent(sykmelding.meta.pasientIdent)
            .map { pasient -> buildSykmeldingHtml(sykmelding, pasient) }
            .map { html ->
                val pdf = pdfGenerator.generatePDFA(html)

                pdfGenerator.verifyPDFACompliance(pdf)

                pdf
            }
    }
}
