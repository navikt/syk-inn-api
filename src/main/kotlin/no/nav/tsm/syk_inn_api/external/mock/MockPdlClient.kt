package no.nav.tsm.syk_inn_api.external.mock

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.common.Result
import no.nav.tsm.syk_inn_api.external.pdl.IDENT_GRUPPE
import no.nav.tsm.syk_inn_api.external.pdl.IPdlClient
import no.nav.tsm.syk_inn_api.external.pdl.Ident
import no.nav.tsm.syk_inn_api.external.pdl.Navn
import no.nav.tsm.syk_inn_api.external.pdl.PdlPerson
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class MockPdlClient : IPdlClient {
    override fun getPerson(fnr: String): Result<PdlPerson> {
        return Result.Success(
            PdlPerson(
                navn =
                    Navn(
                        fornavn = "Ola",
                        mellomnavn = null,
                        etternavn = "Nordmann",
                    ),
                foedselsdato = LocalDate.of(1991, 4, 12),
                identer =
                    listOf(
                        Ident(
                            ident = "12345678901",
                            gruppe = IDENT_GRUPPE.FOLKEREGISTERIDENT,
                            historisk = false,
                        ),
                    ),
            )
        )
    }
}
