package no.nav.tsm.syk_inn_api.pdf

import java.util.*
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController

@RestController
class PdfController(private val pdfService: PdfService) {

    @GetMapping("/api/sykmelding/{sykmeldingId}/pdf", produces = ["application/pdf"])
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
        ) {
            ResponseEntity.status(500).body("Internal server error: ${it.message}")
        }
    }
}
