package no.nav.tsm.sykinnapi.service.syfohelsenettproxy

import no.nav.tsm.sykinnapi.client.syfohelsenettproxy.SyfohelsenettproxyClient
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class SyfohelsenettproxyService(
    private val syfohelsenettproxyClient: SyfohelsenettproxyClient,
) {

    private val logger = LoggerFactory.getLogger(SyfohelsenettproxyService::class.java)

    fun getBehandlerByHpr(behandlerHpr: String, sykmeldingId: String): Behandler {
        logger.info("Trying to fetch behandler for hpr=$behandlerHpr, sykmeldingId=$sykmeldingId")

        val behandler = syfohelsenettproxyClient.getBehandlerByHpr(behandlerHpr, sykmeldingId)

        return behandler
    }
}
