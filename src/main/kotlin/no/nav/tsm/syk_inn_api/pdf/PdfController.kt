package no.nav.tsm.syk_inn_api.pdf

import java.util.*
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class PdfController(private val pdfService: PdfService) {

    private val logger = org.slf4j.LoggerFactory.getLogger(this::class.java)

    @GetMapping("/api/sykmelding/{sykmeldingId}/pdf", produces = [MediaType.APPLICATION_PDF_VALUE])
    fun getSykmeldingPdf(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String
    ): ResponseEntity<Any> {
        val createdPdf =
            pdfService.createSykmeldingPdf(
                sykmeldingId = sykmeldingId,
                hpr = hpr,
            )

        return createdPdf.fold(
            { pdf ->
                if (pdf == null) {
                    ResponseEntity.status(404).body("Sykmelding not found or PDF generation failed")
                } else {
                    ResponseEntity.ok().body(pdf)
                }
            },
        ) { error ->
            logger.error(
                "Failed to create PDF for sykmeldingId: $sykmeldingId, error: ${error.message}",
            )
            ResponseEntity.status(500).body("Internal server error")
        }
    }
}
