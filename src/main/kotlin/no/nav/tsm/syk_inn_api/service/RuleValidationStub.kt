package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.model.SykmeldingPayload
import org.springframework.http.ResponseEntity

class RuleValidationStub {

    fun validateRules(payload: SykmeldingPayload): Boolean {
        return true
    }
}
