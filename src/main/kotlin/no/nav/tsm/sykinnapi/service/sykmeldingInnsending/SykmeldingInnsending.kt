package no.nav.tsm.sykinnapi.service.sykmeldingInnsending

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import no.nav.tsm.sykinnapi.controllers.MissingDataException
import no.nav.tsm.sykinnapi.controllers.SykmeldingApiController
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import no.nav.tsm.sykinnapi.service.syfohelsenettproxy.SyfohelsenettproxyService
import no.nav.tsm.sykinnapi.service.syfosmregler.SyfosmreglerService
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingInnsending(
    val sykmeldingService: SykmeldingService,
    val syfohelsenettproxyService: SyfohelsenettproxyService,
    val syfosmreglerService: SyfosmreglerService,
    val receivedSykmeldingMapper: ReceivedSykmeldingMapper,
    val objectMapper: ObjectMapper
) {

    private val securelog = LoggerFactory.getLogger("securelog")
    private val logger = LoggerFactory.getLogger(SykmeldingApiController::class.java)

    fun send(sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload): String {

        securelog.info(
            "sykInnApiNySykmeldingPayload is: ${
                objectMapper.writeValueAsString(
                    sykInnApiNySykmeldingPayload,
                )
            }",
        )

        val sykmeldingId = UUID.randomUUID().toString()

        val sykmelderBehandler =
            syfohelsenettproxyService.getBehandlerByHpr(
                sykInnApiNySykmeldingPayload.sykmelderHpr,
                sykmeldingId,
            )

        if (sykmelderBehandler.fnr == null) {
            throw MissingDataException("sykmelder mangler fnr!")
        } else {

            val receivedSykmelding =
                receivedSykmeldingMapper.mapToReceivedSykmelding(
                    sykInnApiNySykmeldingPayload,
                    sykmelderBehandler.fnr,
                    sykmeldingId,
                )

            val validationResult = syfosmreglerService.validate(receivedSykmelding)

            val receivedSykmeldingWithValidationResult =
                receivedSykmeldingMapper.mapToReceivedSykmeldingWithValidationResult(
                    receivedSykmelding,
                    validationResult,
                )

            val sykmeldingid =
                sykmeldingService.sendToOkTopic(receivedSykmeldingWithValidationResult)

            logger.info(
                "sykmeldingid with id $sykmeldingid is created and forwarded to the internal systems",
            )

            return sykmeldingid
        }
    }
}
