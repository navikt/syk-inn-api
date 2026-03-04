package no.nav.tsm.modules.external.clients.pdl

import io.ktor.client.*
import io.ktor.server.plugins.di.annotations.*
import java.time.LocalDate
import modules.external.clients.pdl.*
import no.nav.tsm.core.Environment
import no.nav.tsm.modules.external.clients.texas.TexasLocalClient

class PdlLocalClient(
    @Named("RetryHttpClient") httpClient: HttpClient,
    texasClient: TexasLocalClient,
    environment: Environment,
) : PdlClient(httpClient, texasClient, environment) {
    override suspend fun getPerson(ident: String): PdlPerson? {
        return PdlPerson(
            navn = PdlNavn(fornavn = "Test", mellomnavn = "Testesen", etternavn = "Testson"),
            foedselsdato = LocalDate.parse("1990-01-01"),
            identer =
                listOf(
                    Ident(
                        ident = "120398120398",
                        gruppe = IDENT_GRUPPE.FOLKEREGISTERIDENT,
                        historisk = false,
                    )
                ),
        )
    }
}
