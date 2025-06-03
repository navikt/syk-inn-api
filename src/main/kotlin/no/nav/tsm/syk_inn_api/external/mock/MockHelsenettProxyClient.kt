package no.nav.tsm.syk_inn_api.external.mock

import no.nav.tsm.syk_inn_api.common.Godkjenning
import no.nav.tsm.syk_inn_api.common.Kode
import no.nav.tsm.syk_inn_api.common.Result
import no.nav.tsm.syk_inn_api.common.Sykmelder
import no.nav.tsm.syk_inn_api.external.helsenett.IHelsenettProxyClient
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class MockHelsenettProxyClient : IHelsenettProxyClient {
    init {
        println("MockHelsenettProxyClient initialized")
    }

    override fun getSykmelderByHpr(behandlerHpr: String, sykmeldingId: String): Result<Sykmelder> {
        return Result.Success(
            Sykmelder(
                godkjenninger =
                    listOf(
                        Godkjenning(
                            helsepersonellkategori = Kode(aktiv = true, oid = 0, verdi = "LE"),
                            autorisasjon = Kode(aktiv = true, oid = 7704, verdi = "1"),
                            tillegskompetanse = null
                        )
                    ),
                fnr = "09099012345",
                hprNummer = "123456789",
                fornavn = "James",
                mellomnavn = "007",
                etternavn = "Bond"
            )
        )
    }
}
