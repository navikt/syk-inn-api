package no.nav.tsm.syk_inn_api.mock

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.client.IPdlClient

import no.nav.tsm.syk_inn_api.model.IDENT_GRUPPE
import no.nav.tsm.syk_inn_api.model.Ident
import no.nav.tsm.syk_inn_api.model.Navn
import no.nav.tsm.syk_inn_api.model.PdlPerson
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local")
@Component
class MockPdlClient : IPdlClient {
    override fun getPerson(fnr: String): Result<PdlPerson> {
        return Result.success(
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
