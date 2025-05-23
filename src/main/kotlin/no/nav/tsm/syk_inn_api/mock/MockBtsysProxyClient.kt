package no.nav.tsm.syk_inn_api.mock

import no.nav.tsm.syk_inn_api.client.IBtsysClient
import no.nav.tsm.syk_inn_api.client.Result
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class MockBtsysProxyClient : IBtsysClient {
    override fun checkSuspensionStatus(
        sykmelderFnr: String,
        oppslagsdato: String
    ): Result<Boolean> {
        return Result.Success(false)
    }
}
