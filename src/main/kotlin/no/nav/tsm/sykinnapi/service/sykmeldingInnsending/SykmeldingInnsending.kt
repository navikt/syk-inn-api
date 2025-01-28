package no.nav.tsm.sykinnapi.service.sykmeldingInnsending

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*
import no.nav.tsm.sykinnapi.controllers.SykmeldingApiController
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Status
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiResponse
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

    fun send(sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload): SykInnApiResponse {

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

        val receivedSykmelding =
            receivedSykmeldingMapper.mapToReceivedSykmelding(
                sykInnApiNySykmeldingPayload,
                sykmelderBehandler.fnr,
                sykmeldingId,
            )

        val validationResult = syfosmreglerService.validate(receivedSykmelding)

        securelog.info(
            "validationResult is for sykmeldingid ${sykmeldingId}: ${
                objectMapper.writeValueAsString(
                    validationResult,
                )
            }",
        )

        val receivedSykmeldingWithValidationResult =
            receivedSykmeldingMapper.mapToReceivedSykmeldingWithValidationResult(
                receivedSykmelding,
                validationResult,
            )

        if (Status.OK == validationResult.status) {
            sykmeldingService.sendToOkTopic(receivedSykmeldingWithValidationResult)

            logger.info(
                "sykmeldingid with id $sykmeldingId is created and forwarded to the internal systems",
            )
        } else {
            logger.warn(
                "sykmeldingid with id $sykmeldingId is not created and not forwarded to" +
                    " the internal systems, validationResult status is :${validationResult.status} " +
                    "rules are: ${objectMapper.writeValueAsString(validationResult.ruleHits)}",
            )
        }

        val sykInnApiResponse =
            SykInnApiResponse(
                sykmeldingId = sykmeldingId,
                validationResult = validationResult,
            )

        return sykInnApiResponse
    }
}
