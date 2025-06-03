package no.nav.tsm.syk_inn_api.external.mock

import no.nav.tsm.syk_inn_api.common.Result
import no.nav.tsm.syk_inn_api.external.btsys.IBtsysClient
import no.nav.tsm.syk_inn_api.external.btsys.Suspendert
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class MockBtsysProxyClient : IBtsysClient {
    override fun checkSuspensionStatus(
        sykmelderFnr: String,
        oppslagsdato: String
    ): Result<Suspendert> {
        return Result.Success(Suspendert(suspendert = false))
    }
}
