package modules.sykmelder.clients.pdl

import java.time.LocalDate

class PdlLocalClient : PdlClient {
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
