package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.client.BtsysProxyClient
import no.nav.tsm.syk_inn_api.client.HelsenettProxyClient
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BtsysProxyService(
    private val btsysProxyClient: BtsysProxyClient,
    proxyClient: BtsysProxyClient
) {
    private val logger = LoggerFactory.getLogger(HelsenettProxyClient::class.java)
    fun isSuspended(sykmelderFnr: String, signaturDato: String): Boolean {
        return btsysProxyClient.checkSuspensionStatus(
            sykmelderFnr = sykmelderFnr,
            oppslagsdato = signaturDato,
        )
    }

}
