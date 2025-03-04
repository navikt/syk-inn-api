package no.nav.tsm.sykinnapi.service.smpdfgen

import java.util.*
import no.nav.tsm.sykinnapi.client.smpdfgen.SmpdfgenClient
import no.nav.tsm.sykinnapi.client.smpdfgen.createPdfPayload
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.tsmpdl.PdlPerson
import org.springframework.stereotype.Service

@Service
class SmPdfGenService(
    private val smpdfgenClient: SmpdfgenClient,
) {
    fun createPdf(receivedSykmelding: ReceivedSykmelding, pdlPerson: PdlPerson): String {
        val pdfgenPayload = createPdfPayload(receivedSykmelding, pdlPerson)

        val pdf = smpdfgenClient.createPdf(pdfgenPayload)

        val base64EncodedPdf = Base64.getEncoder().encodeToString(pdf)

        return base64EncodedPdf
    }
}
