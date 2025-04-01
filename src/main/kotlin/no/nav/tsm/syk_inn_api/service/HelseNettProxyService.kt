package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.client.HelsenettProxyClient
import no.nav.tsm.syk_inn_api.model.Sykmelder
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class HelseNettProxyService(
    private val helsenettProxyClient: HelsenettProxyClient,
) {
    private val logger = LoggerFactory.getLogger(HelsenettProxyClient::class.java)

    fun getSykmelderByHpr(hpr: String, sykmeldingId: String): Sykmelder {
        logger.info("Getting sykmelder for hpr=$hpr, sykmeldingId=$sykmeldingId") //TODO ok to log hprnummer?
        return helsenettProxyClient.getSykmelderByHpr(hpr, sykmeldingId)
    }

}
