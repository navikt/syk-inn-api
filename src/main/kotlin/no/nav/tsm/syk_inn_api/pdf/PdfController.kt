package no.nav.tsm.syk_inn_api.pdf

import java.util.*
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("api/sykmelding")
class PdfController(private val pdfService: PdfService) {

    private val logger = logger()

    @GetMapping(
        "/{sykmeldingId}/pdf",
        produces = [MediaType.APPLICATION_PDF_VALUE, MediaType.TEXT_PLAIN_VALUE]
    )
    fun getSykmeldingPdf(
        @PathVariable sykmeldingId: UUID,
        @RequestHeader("HPR") hpr: String
    ): ResponseEntity<Any> {
        if (hpr.isEmpty()) {
            logger.warn("HPR mangler for sykmeldingId $sykmeldingId")
            return ResponseEntity.badRequest().body("Missing HPR parameter")
        }

        logger.info("Creating PDF for ID $sykmeldingId (HPR: $hpr)")

        val createdPdf =
            pdfService.createSykmeldingPdf(
                sykmeldingId = sykmeldingId,
                hpr = hpr,
            )

        return createdPdf.fold(
            { pdf ->
                if (pdf == null) {
                    ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("Sykmelding not found or PDF generation failed")
                } else {
                    ResponseEntity.ok().body(pdf)
                }
            },
        ) { error ->
            logger.error(
                "Failed to create PDF for sykmeldingId: $sykmeldingId, error: ${error.message}",
            )
            ResponseEntity.internalServerError().body("Internal server error")
        }
    }
}
