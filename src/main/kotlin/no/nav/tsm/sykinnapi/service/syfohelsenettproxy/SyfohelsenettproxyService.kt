package no.nav.tsm.sykinnapi.service.syfohelsenettproxy

import no.nav.tsm.sykinnapi.client.SyfohelsenettproxyClient
import no.nav.tsm.sykinnapi.modell.syfohelsenettproxy.Behandler
import org.springframework.stereotype.Service

@Service
class SyfohelsenettproxyService(private val syfohelsenettproxyClient: SyfohelsenettproxyClient) {

    fun getBehandlerByHpr(behandlerHpr: String, sykmeldingId: String): Behandler {

        val behandler = syfohelsenettproxyClient.getBehandlerByHpr(behandlerHpr, sykmeldingId)

        // Replace with client call, behandler
        return Behandler(
            godkjenninger = emptyList(),
            fnr = "12345678912",
            hprNummer = behandlerHpr,
            fornavn = "Fornavn",
            mellomnavn = null,
            etternavn = "etternavn",
        )
    }
}
