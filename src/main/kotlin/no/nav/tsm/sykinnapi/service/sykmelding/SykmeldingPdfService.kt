package no.nav.tsm.sykinnapi.service.sykmelding

import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import no.nav.tsm.sykinnapi.service.smpdfgen.SmPdfGenService
import no.nav.tsm.sykinnapi.service.syfosmregister.SyfosmregisterService
import no.nav.tsm.sykinnapi.service.tsmpdl.TsmPdlService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingPdfService(
    private val syfosmregisterService: SyfosmregisterService,
    private val smPdfGenService: SmPdfGenService,
    private val receivedSykmeldingMapper: ReceivedSykmeldingMapper,
    private val tsmPdlService: TsmPdlService,
) {
    private val securelog = LoggerFactory.getLogger("securelog")
    private val logger = LoggerFactory.getLogger(SykmeldingPdfService::class.java)

    fun getPdf(sykmeldingId: String, hpr: String): ByteArray {
        logger.info(
            "Trying to fetch sykmelding for sykmeldingId=$sykmeldingId, hprNummer=$hpr",
        )
        val syfoSmRegisterSykmelding = syfosmregisterService.getSykmelding(sykmeldingId)
        if (syfoSmRegisterSykmelding.behandler.hpr != hpr) {
            throw RuntimeException("HPR-nummer matcher ikke behandler")
        }

        logger.info("Trying to fetch pdlPerson for sykmeldingId=$sykmeldingId")
        securelog.info(
            "Trying to fetch pdlPerson for sykmeldingId=$sykmeldingId and ident=${syfoSmRegisterSykmelding.pasient.fnr}",
        )

        val pdlPerson = tsmPdlService.getPdlPerson(syfoSmRegisterSykmelding.pasient.fnr)
        val receivedSykmelding = receivedSykmeldingMapper.mapSykmeldingDTOToReceivedSykmelding(
            syfoSmRegisterSykmelding,
            syfoSmRegisterSykmelding.behandler.fnr,
            sykmeldingId,
        )

        return smPdfGenService.createPdf(receivedSykmelding, pdlPerson)
    }
}
