package no.nav.tsm.syk_inn_api.kafka

import no.nav.tsm.syk_inn_api.model.SykmeldingPayload

class KafkaStubber {
    fun sendToOpprettSykmeldingTopic(payload: SykmeldingPayload): Boolean {
        return true
    }

    fun getSykmeldingWithUtfall(id: String): Boolean {
        return true
    }
}
