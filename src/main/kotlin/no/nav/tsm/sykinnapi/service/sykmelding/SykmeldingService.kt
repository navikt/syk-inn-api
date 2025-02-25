package no.nav.tsm.sykinnapi.service.sykmelding

import com.fasterxml.jackson.databind.ObjectMapper
import java.time.LocalDate
import java.util.*
import no.nav.tsm.sykinnapi.config.kafka.SykmeldingOKProducer
import no.nav.tsm.sykinnapi.controllers.SykmeldingApiController
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmeldingWithValidationResult
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Status
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiResponse
import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import no.nav.tsm.sykinnapi.service.syfohelsenettproxy.SyfohelsenettproxyService
import no.nav.tsm.sykinnapi.service.syfosmregister.SyfosmregisterService
import no.nav.tsm.sykinnapi.service.syfosmregler.SyfosmreglerService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SykmeldingService(
    val syfosmregisterService: SyfosmregisterService,
    val sykmeldingOKProducer: SykmeldingOKProducer,
    val syfohelsenettproxyService: SyfohelsenettproxyService,
    val syfosmreglerService: SyfosmreglerService,
    val receivedSykmeldingMapper: ReceivedSykmeldingMapper,
    val objectMapper: ObjectMapper
) {

    private val securelog = LoggerFactory.getLogger("securelog")
    private val logger = LoggerFactory.getLogger(SykmeldingApiController::class.java)

    fun getSykmeldingKvittering(sykmeldingId: String, hprNummer: String): SykmeldingKvittering {
        logger.info(
            "Trying to fetch sykmelding for sykmeldingId=$sykmeldingId, hprNummer=$hprNummer",
        )
        val sykmeldingDTO = syfosmregisterService.getSykmelding(sykmeldingId)

        if (sykmeldingDTO.behandler.hpr == hprNummer) {
            return SykmeldingKvittering(
                sykmeldingId = sykmeldingId,
                periode =
                    Periode(
                        fom = sykmeldingDTO.periode.fom,
                        tom = sykmeldingDTO.periode.tom,
                    ),
                pasient = Pasient(fnr = sykmeldingDTO.pasient.fnr),
                hovedDiagnose =
                    Diagnose(
                        code = sykmeldingDTO.hovedDiagnose.code,
                        system = sykmeldingDTO.hovedDiagnose.system,
                        text = sykmeldingDTO.hovedDiagnose.text,
                    ),
            )
        } else {
            throw RuntimeException("HPR-nummer matcher ikke behandler")
        }
    }

    fun sendSykmelding(
        sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload
    ): SykInnApiResponse {

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
            sendToOkTopic(receivedSykmeldingWithValidationResult)

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

    fun sendToOkTopic(
        receivedSykmeldingWithValidationResult: ReceivedSykmeldingWithValidationResult,
    ) {
        sykmeldingOKProducer.send(receivedSykmeldingWithValidationResult)
    }

    fun getSykmeldingByIdent(ident: String): List<SykmeldingKvittering> {
        logger.info(
            "Trying to fetch sykmelding for ident=$ident",
        )
        val sykmeldingDTO = syfosmregisterService.getSykmeldingByIdent(ident)

        return sykmeldingDTO.map { sykmelding ->
            SykmeldingKvittering(
                sykmeldingId = sykmelding.sykmeldingId,
                periode =
                    Periode(
                        fom = sykmelding.periode.fom,
                        tom = sykmelding.periode.tom,
                    ),
                pasient = Pasient(fnr = sykmelding.pasient.fnr),
                hovedDiagnose =
                    Diagnose(
                        code = sykmelding.hovedDiagnose.code,
                        system = sykmelding.hovedDiagnose.system,
                        text = sykmelding.hovedDiagnose.text,
                    ),
            )
        }
    }
}

data class SykmeldingKvittering(
    val sykmeldingId: String,
    val periode: Periode,
    val pasient: Pasient,
    val hovedDiagnose: Diagnose
)

data class Periode(val fom: LocalDate, val tom: LocalDate)

data class Pasient(val fnr: String)

data class Diagnose(val code: String, val system: String, val text: String)
