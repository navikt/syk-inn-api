package no.nav.tsm.syk_inn_api.person

import no.nav.tsm.syk_inn_api.person.pdl.IDENT_GRUPPE
import no.nav.tsm.syk_inn_api.person.pdl.IPdlClient
import no.nav.tsm.syk_inn_api.person.pdl.PdlPerson
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import org.springframework.stereotype.Service

@Service
class PersonService(
    private val pdlClient: IPdlClient,
) {
    private val logger = logger()
    private val teamLog = teamLogger()

    fun getPersonByIdent(ident: String): Result<Person> {
        val person: PdlPerson =
            pdlClient.getPerson(ident).fold({ it }) {
                teamLog.error("Error while fetching person info for fnr=$ident", it)
                logger.error("Error while fetching person info from PDL, check secure logs")
                return Result.failure(it)
            }

        val currentIdent =
            person.identer
                .find { it.gruppe == IDENT_GRUPPE.FOLKEREGISTERIDENT && !it.historisk }
                ?.ident

        if (currentIdent == null) {
            return Result.failure(
                IllegalStateException(
                    "No valid FOLKEREGISTERIDENT found for person with ident $ident"
                )
            )
        }

        if (person.navn == null) {
            return Result.failure(
                IllegalStateException("No name found for person with ident $ident")
            )
        }

        if (person.foedselsdato == null) {
            teamLog.error("No fødselsdato found for person with ident $ident")
            return Result.failure(
                IllegalStateException("Found person without fødselsdato, see teamlog for ident")
            )
        }

        return Result.success(
            Person(
                navn = person.navn,
                ident = currentIdent,
                fodselsdato = person.foedselsdato,
            )
        )
    }
}
