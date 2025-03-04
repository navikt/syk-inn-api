package no.nav.tsm.sykinnapi.service.smpdfgen

import no.nav.tsm.sykinnapi.client.smpdfgen.SmpdfgenClient
import no.nav.tsm.sykinnapi.client.smpdfgen.createPdfPayload
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.tsmpdl.PdlPerson
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingPdfService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SmPdfGenService(
    private val smpdfgenClient: SmpdfgenClient,
) {
    private val logger = LoggerFactory.getLogger(SykmeldingPdfService::class.java)

    fun createPdf(receivedSykmelding: ReceivedSykmelding, pdlPerson: PdlPerson): ByteArray {
        logger.info("Trying to generate pdf for sykmeldingId=${receivedSykmelding.sykmelding.id}")
        val pdfgenPayload = createPdfPayload(receivedSykmelding, pdlPerson)

        return smpdfgenClient.createPdf(pdfgenPayload).also {
            logger.info("Pdf is created for sykmeldingId=${receivedSykmelding.sykmelding.id}")
        }
    }
}
