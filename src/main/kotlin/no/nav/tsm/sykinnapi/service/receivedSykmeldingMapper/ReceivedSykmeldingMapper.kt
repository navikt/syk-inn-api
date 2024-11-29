package no.nav.tsm.sykinnapi.service.receivedSykmeldingMapper

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import no.nav.tsm.sykinnapi.mapper.receivedSykmeldingMapper
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmeldingWithValidationResult
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.toReceivedSykmeldingWithValidation
import no.nav.tsm.sykinnapi.modell.sykinn.SykInnApiNySykmeldingPayload
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ReceivedSykmeldingMapper {

    private val securelog = LoggerFactory.getLogger("securelog")

    fun mapToReceivedSykmelding(
        sykInnApiNySykmeldingPayload: SykInnApiNySykmeldingPayload,
        sykmelderFnr: String,
        sykmeldingId: String
    ): ReceivedSykmelding {

        val receivedSykmelding =
            receivedSykmeldingMapper(
                sykInnApiNySykmeldingPayload,
                sykmelderFnr,
                sykmeldingId,
            )

        securelog.info(
            "Successfully created receivedSykmelding: ${
                ObjectMapper().apply {
                    registerKotlinModule()
                    registerModule(JavaTimeModule())
                }.writeValueAsString(receivedSykmelding)
            }",
        )

        return receivedSykmelding
    }

    fun mapToReceivedSykmeldingWithValidationResult(
        receivedSykmelding: ReceivedSykmelding,
        validationResult: ValidationResult
    ): ReceivedSykmeldingWithValidationResult {
        return receivedSykmelding.toReceivedSykmeldingWithValidation(validationResult)
    }
}
