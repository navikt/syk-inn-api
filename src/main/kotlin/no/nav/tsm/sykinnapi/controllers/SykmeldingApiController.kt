package no.nav.tsm.sykinnapi.controllers

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.UUID
import no.nav.security.token.support.core.api.ProtectedWithClaims
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.toReceivedSykmeldingWithValidation
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import no.nav.tsm.sykinnapi.service.syfohelsenettproxy.SyfohelsenettproxyService
import no.nav.tsm.sykinnapi.service.syfosmregler.SyfosmreglerService
import no.nav.tsm.sykinnapi.service.sykmelding.SykmeldingService
import org.slf4j.LoggerFactory
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController

@ProtectedWithClaims(issuer = "azuread")
@RestController
class SykmeldingApiController(
    val sykmeldingService: SykmeldingService,
    val syfohelsenettproxyService: SyfohelsenettproxyService,
    val syfosmreglerService: SyfosmreglerService,
    val receivedSykmeldingMapper: ReceivedSykmeldingMapper
) {
    private val securelog = LoggerFactory.getLogger("securelog")
    private val logger = LoggerFactory.getLogger(SykmeldingApiController::class.java)

    @PostMapping("/api/v1/sykmelding/create")
    fun createSykmelding(
        @RequestBody sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload,
    ): String {

        securelog.info(
            "sykInnApiNySykmeldingPayload is: ${
                ObjectMapper().writeValueAsString(
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
                receivedSykmelding.toReceivedSykmeldingWithValidation(validationResult)

            val sykmeldingid =
                sykmeldingService.sendToOkTopic(receivedSykmeldingWithValidationResult)

            logger.info(
                "sykmeldingid with id $sykmeldingid is created",
            )

            return sykmeldingid
        }
    }
}

class MissingDataException(override val message: String) : Exception(message)
