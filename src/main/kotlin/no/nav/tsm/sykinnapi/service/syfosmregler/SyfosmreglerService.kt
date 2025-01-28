package no.nav.tsm.sykinnapi.service.syfosmregler

import no.nav.tsm.sykinnapi.client.syfosmregler.SyfosmreglerClient
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ReceivedSykmelding
import no.nav.tsm.sykinnapi.modell.receivedSykmelding.ValidationResult
import org.springframework.stereotype.Service

@Service
class SyfosmreglerService(
    private val syfosmreglerClient: SyfosmreglerClient,
) {

    fun validate(receivedSykmelding: ReceivedSykmelding): ValidationResult {

        val validationResult = syfosmreglerClient.validate(receivedSykmelding)

        return validationResult
    }
}
