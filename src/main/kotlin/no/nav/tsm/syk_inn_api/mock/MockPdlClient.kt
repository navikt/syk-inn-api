package no.nav.tsm.syk_inn_api.mock

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.client.IPdlClient
import no.nav.tsm.syk_inn_api.client.Result
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class MockPdlClient : IPdlClient {
    override fun getFodselsdato(fnr: String): Result<LocalDate> {
        return Result.Success(LocalDate.of(1991, 0, 12))
    }
}
