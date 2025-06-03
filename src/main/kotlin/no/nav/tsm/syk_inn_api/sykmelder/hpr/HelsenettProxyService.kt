package no.nav.tsm.syk_inn_api.sykmelder.hpr

import org.springframework.stereotype.Service

@Service
class HelsenettProxyService(
    private val helsenettProxyClient: IHelsenettProxyClient,
) {

    fun getSykmelderByHpr(hpr: String, sykmeldingId: String): Result<HprSykmelder> =
        helsenettProxyClient.getSykmelderByHpr(hpr, sykmeldingId)
}
