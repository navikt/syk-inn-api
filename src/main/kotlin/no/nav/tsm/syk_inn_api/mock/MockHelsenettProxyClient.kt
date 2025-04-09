package no.nav.tsm.syk_inn_api.mock

import no.nav.tsm.syk_inn_api.client.IHelsenettProxyClient
import no.nav.tsm.syk_inn_api.client.Result
import no.nav.tsm.syk_inn_api.model.Godkjenning
import no.nav.tsm.syk_inn_api.model.GyldighetsPeriode
import no.nav.tsm.syk_inn_api.model.Kode
import no.nav.tsm.syk_inn_api.model.Sykmelder
import no.nav.tsm.syk_inn_api.model.Tilleggskompetanse
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Profile("local")
@Component
class MockHelsenettProxyClient : IHelsenettProxyClient {
    init {
        println("MockHelsenettProxyClient initialized")
    }
    override fun getSykmelderByHpr(behandlerHpr: String, sykmeldingId: String): Result<Sykmelder> {
        return Result.Success(
            Sykmelder(
                godkjenninger = listOf(
                    Godkjenning(
                        helsepersonellkategori = Kode(
                            aktiv = true,
                            oid = 0,
                            verdi = "LE"
                        ),
                        autorisasjon = Kode(
                            aktiv = true,
                            oid = 7704,
                            verdi = "1"
                        ),
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
