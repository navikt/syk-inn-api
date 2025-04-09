package no.nav.tsm.syk_inn_api.mock

import no.nav.tsm.syk_inn_api.client.IHelsenettProxyClient
import no.nav.tsm.syk_inn_api.client.Result
import no.nav.tsm.syk_inn_api.model.Sykmelder
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class MockHelsenettProxyClient : IHelsenettProxyClient {
    override fun getSykmelderByHpr(behandlerHpr: String, sykmeldingId: String): Result<Sykmelder> {
        return Result.Success(
            Sykmelder(
                godkjenninger = emptyList(),
                fnr = "09090012345",
                hprNummer = "123456789",
                fornavn = "James",
                mellomnavn = "007",
                etternavn = "Bond"
            )
        )
    }
}
