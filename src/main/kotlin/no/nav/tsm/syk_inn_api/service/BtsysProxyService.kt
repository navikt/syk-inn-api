package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.client.HelsenettProxyClient
import no.nav.tsm.syk_inn_api.client.IBtsysClient
import no.nav.tsm.syk_inn_api.client.Result
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class BtsysProxyService(
    private val btsysProxyClient: IBtsysClient,
) {
    private val logger = LoggerFactory.getLogger(HelsenettProxyClient::class.java)
    private val secureLog: Logger = LoggerFactory.getLogger("securelog")

    fun isSuspended(sykmelderFnr: String, signaturDato: String): Boolean {
        return when (
            val result =
                btsysProxyClient.checkSuspensionStatus(
                    sykmelderFnr = sykmelderFnr,
                    oppslagsdato = signaturDato,
                )
        ) {
            is Result.Success -> result.data
            is Result.Failure -> {
                secureLog.error(
                    "Error while checking suspension status for sykmelderFnr=$sykmelderFnr",
                    result.error
                )
                throw result.error
            }
        }
    }
}
