package no.nav.tsm.syk_inn_api.sykmelder.btsys

import java.time.LocalDate
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "test")
@Component
class MockBtsysClient : IBtsysClient {
    override fun checkSuspensionStatus(
        sykmelderFnr: String,
        oppslagsdato: LocalDate
    ): Result<Suspendert> {
        return Result.success(Suspendert(suspendert = false))
    }
}
