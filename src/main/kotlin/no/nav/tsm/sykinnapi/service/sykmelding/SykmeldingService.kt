package no.nav.tsm.sykinnapi.service.sykmelding

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.tsm.sykinnapi.config.kafka.SykmeldingOKProducer
import no.nav.tsm.sykinnapi.controllers.SykmeldingApiController
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmeldingWithValidationResult
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Status
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiResponse
import no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper.ReceivedSykmeldingMapper
import no.nav.tsm.sykinnapi.service.smpdfgen.SmPdfGenService
import no.nav.tsm.sykinnapi.service.syfohelsenettproxy.SyfohelsenettproxyService
import no.nav.tsm.sykinnapi.service.syfosmregister.SyfosmregisterService
import no.nav.tsm.sykinnapi.service.syfosmregler.SyfosmreglerService
import no.nav.tsm.sykinnapi.service.tsmpdl.TsmPdlService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.*

@Service
class SykmeldingService(
    val syfosmregisterService: SyfosmregisterService,
    val sykmeldingOKProducer: SykmeldingOKProducer,
    val syfohelsenettproxyService: SyfohelsenettproxyService,
    val syfosmreglerService: SyfosmreglerService,
    val smPdfGenService: SmPdfGenService,
    val tsmPdlService: TsmPdlService,
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

        logger.info("Trying to fetch pdlPerson for sykmeldingId=$sykmeldingId")

        securelog.info(
            "Trying to fetch pdlPerson for sykmeldingId=$sykmeldingId and ident=${sykmeldingDTO.pasient.fnr}"
        )

        val pdlPerson = tsmPdlService.getPdlPerson(sykmeldingDTO.pasient.fnr)

        val receivedSykmelding =
            receivedSykmeldingMapper.mapSykmeldingDTOToReceivedSykmelding(
                sykmeldingDTO,
                sykmeldingDTO.behandler.fnr,
                sykmeldingId,
            )

        logger.info("Trying to generate pdf for sykmeldingId=$sykmeldingId")

        val pdfByteArray = smPdfGenService.createPdf(receivedSykmelding, pdlPerson)

        logger.info("Pdf is created for sykmeldingId=$sykmeldingId")

        if (sykmeldingDTO.behandler.hpr == hprNummer) {
            return SykmeldingKvittering(
                sykmeldingId = sykmeldingId,
                aktivitet =
                    when (sykmeldingDTO.aktivitet) {
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.AktivitetIkkeMulig ->
                            Aktivitet.AktivitetIkkeMulig(
                                fom = sykmeldingDTO.aktivitet.fom,
                                tom = sykmeldingDTO.aktivitet.tom,
                            )
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Gradert ->
                            Aktivitet.Gradert(
                                grad = sykmeldingDTO.aktivitet.grad,
                                fom = sykmeldingDTO.aktivitet.fom,
                                tom = sykmeldingDTO.aktivitet.tom,
                            )
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Avvetende ->
                            Aktivitet.Avvetende(
                                fom = sykmeldingDTO.aktivitet.fom,
                                tom = sykmeldingDTO.aktivitet.tom,
                            )
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Behandlingsdager ->
                            Aktivitet.Behandlingsdager(
                                fom = sykmeldingDTO.aktivitet.fom,
                                tom = sykmeldingDTO.aktivitet.tom,
                            )
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Reisetilskudd ->
                            Aktivitet.Reisetilskudd(
                                fom = sykmeldingDTO.aktivitet.fom,
                                tom = sykmeldingDTO.aktivitet.tom,
                            )
                    },
                pasient = Pasient(fnr = sykmeldingDTO.pasient.fnr),
                hovedDiagnose =
                    Diagnose(
                        code = sykmeldingDTO.hovedDiagnose.code,
                        system = sykmeldingDTO.hovedDiagnose.system,
                        text = sykmeldingDTO.hovedDiagnose.text,
                    ),
                pdf = pdfByteArray
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

    fun getSykmeldingerByIdent(ident: String): List<SykmeldingHistorikk> {
        securelog.info(
            "Trying to fetch sykmelding for ident=$ident",
        )
        val sykmeldingDTO = syfosmregisterService.getSykmeldingByIdent(ident)

        return sykmeldingDTO.map { sykmelding ->
            SykmeldingHistorikk(
                sykmeldingId = sykmelding.sykmeldingId,
                aktivitet =
                    when (sykmelding.aktivitet) {
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.AktivitetIkkeMulig ->
                            Aktivitet.AktivitetIkkeMulig(
                                fom = sykmelding.aktivitet.fom,
                                tom = sykmelding.aktivitet.tom,
                            )
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Gradert ->
                            Aktivitet.Gradert(
                                grad = sykmelding.aktivitet.grad,
                                fom = sykmelding.aktivitet.fom,
                                tom = sykmelding.aktivitet.tom,
                            )
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Avvetende ->
                            Aktivitet.Avvetende(
                                fom = sykmelding.aktivitet.fom,
                                tom = sykmelding.aktivitet.tom,
                            )
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Behandlingsdager ->
                            Aktivitet.Behandlingsdager(
                                fom = sykmelding.aktivitet.fom,
                                tom = sykmelding.aktivitet.tom,
                            )
                        is no.nav.tsm.sykinnapi.modell.syfosmregister.Aktivitet.Reisetilskudd ->
                            Aktivitet.Reisetilskudd(
                                fom = sykmelding.aktivitet.fom,
                                tom = sykmelding.aktivitet.tom,
                            )
                    },
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

data class SykmeldingHistorikk(
    val sykmeldingId: String,
    val aktivitet: Aktivitet,
    val pasient: Pasient,
    val hovedDiagnose: Diagnose,
)

data class SykmeldingKvittering(
    val sykmeldingId: String,
    val aktivitet: Aktivitet,
    val pasient: Pasient,
    val hovedDiagnose: Diagnose,
    val pdf: ByteArray
)

data class Pasient(val fnr: String)

data class Diagnose(val code: String, val system: String, val text: String)

@JsonSubTypes(
    JsonSubTypes.Type(Aktivitet.AktivitetIkkeMulig::class, name = "AKTIVITET_IKKE_MULIG"),
    JsonSubTypes.Type(Aktivitet.Gradert::class, name = "GRADERT"),
    JsonSubTypes.Type(Aktivitet.Avvetende::class, name = "AVVENTENDE"),
    JsonSubTypes.Type(Aktivitet.Behandlingsdager::class, name = "BEHANDLINGSDAGER"),
    JsonSubTypes.Type(Aktivitet.Reisetilskudd::class, name = "REISETILSKUDD"),
)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
sealed interface Aktivitet {
    data class AktivitetIkkeMulig(val fom: LocalDate, val tom: LocalDate) : Aktivitet

    data class Gradert(val grad: Int, val fom: LocalDate, val tom: LocalDate) : Aktivitet

    data class Avvetende(val fom: LocalDate, val tom: LocalDate) : Aktivitet

    data class Behandlingsdager(val fom: LocalDate, val tom: LocalDate) : Aktivitet

    data class Reisetilskudd(val fom: LocalDate, val tom: LocalDate) : Aktivitet
}
