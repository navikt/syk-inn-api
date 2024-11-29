package no.nav.tsm.sykinnapi.service.syfosmregler

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.tsm.sykinnapi.client.SyfosmreglerClient
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.Status
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import org.springframework.stereotype.Service

@Service
class SyfosmreglerService(
    private val syfosmreglerClient: SyfosmreglerClient,
) {

    fun validate(receivedSykmelding: ReceivedSykmelding): ValidationResult {

        val validationResult = syfosmreglerClient.validate(receivedSykmelding)
        if (validationResult.status == Status.OK) {
            return validationResult
        }

        throw ValidationResultException(
            "validationResult status is :${validationResult.status} " +
                "rules are: ${jacksonObjectMapper().writeValueAsString(validationResult.ruleHits)}"
        )
    }
}

class ValidationResultException(override val message: String) : Exception(message)
