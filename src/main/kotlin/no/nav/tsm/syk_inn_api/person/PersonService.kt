package no.nav.tsm.syk_inn_api.person

import no.nav.tsm.syk_inn_api.client.Result
import no.nav.tsm.syk_inn_api.person.pdl.IDENT_GRUPPE
import no.nav.tsm.syk_inn_api.person.pdl.PdlPerson
import no.nav.tsm.syk_inn_api.person.pdl.IPdlClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val pdlClient: IPdlClient,
) {
    private val logger = LoggerFactory.getLogger(PersonService::class.java)
    private val secureLog: Logger = LoggerFactory.getLogger("securelog")

    fun getPersonByIdent(ident: String): Person {
        val person: PdlPerson =
            when (val result = pdlClient.getPerson(ident)) {
                is Result.Success -> result.data
                is Result.Failure -> {
                    secureLog.error("Error while fetching person info for fnr=$ident", result.error)
                    logger.error("Error while fetching person info from PDL, check secure logs")
                    throw result
                        .error // should we handle the flow differently ? or use the throw here?
                }
            }

        val currentIdent =
            person.identer
                .find { it.gruppe == IDENT_GRUPPE.FOLKEREGISTERIDENT && !it.historisk }
                ?.ident
                ?: throw IllegalStateException(
                    "No valid FOLKEREGISTERIDENT found for person with ident $ident"
                )

        val personNavn =
            person.navn ?: throw IllegalStateException("No name found for person with ident $ident")

        return Person(
            navn = personNavn,
            ident = currentIdent,
            fodselsdato = person.foedselsdato,
        )
    }
}
