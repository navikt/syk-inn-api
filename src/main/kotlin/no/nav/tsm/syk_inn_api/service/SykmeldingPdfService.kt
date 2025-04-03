package no.nav.tsm.syk_inn_api.service

import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service

@Service
class SykmeldingPdfService {
    fun getPdf(sykmeldingId: String, hpr: String): ResponseEntity<Any> {

        // TODO implement
        return ResponseEntity("pdf".toByteArray(), null, 200)
    }
}
