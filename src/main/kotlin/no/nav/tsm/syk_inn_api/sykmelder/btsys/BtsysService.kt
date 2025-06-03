package no.nav.tsm.syk_inn_api.sykmelder.btsys

import org.springframework.stereotype.Service

@Service
class BtsysService(
    private val btsysClient: IBtsysClient,
) {
    fun isSuspended(sykmelderFnr: String, signaturDato: String): Result<Boolean> =
        btsysClient
            .checkSuspensionStatus(
                sykmelderFnr = sykmelderFnr,
                oppslagsdato = signaturDato,
            )
            .map { it.suspendert }
}
