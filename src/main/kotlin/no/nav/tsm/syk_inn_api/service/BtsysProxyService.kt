package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.client.HelsenettProxyClient
import no.nav.tsm.syk_inn_api.client.IBtsysClient

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BtsysProxyService(
    private val btsysProxyClient: IBtsysClient,
) {
    private val secureLog: Logger = LoggerFactory.getLogger("securelog")

    fun isSuspended(sykmelderFnr: String, signaturDato: String): Boolean {
        val result = btsysProxyClient.checkSuspensionStatus(
            sykmelderFnr = sykmelderFnr,
            oppslagsdato = signaturDato,
        )

        return result.fold({ it }) {
            secureLog.error(
                "Error while checking suspension status for sykmelderFnr=$sykmelderFnr", it,
            )
            throw it
        }
    }
}

