package no.nav.tsm.syk_inn_api.person

import no.nav.tsm.syk_inn_api.person.pdl.IDENT_GRUPPE
import no.nav.tsm.syk_inn_api.person.pdl.IPdlClient
import no.nav.tsm.syk_inn_api.person.pdl.PdlPerson
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.teamLogger
import org.springframework.stereotype.Service

class PdlException(message: String) : Exception(message)

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
            teamLog.error("No valid FOLKEREGISTERIDENT found for person with ident $ident")
            return Result.failure(
                PdlException("No valid FOLKEREGISTERIDENT found for person, see teamlog for ident")
            )
        }

        if (person.navn == null) {
            teamLog.error("No name found for person with ident $ident")
            return Result.failure(PdlException("No name found for person, see teamlog for ident"))
        }

        if (person.foedselsdato == null) {
            teamLog.error("No fødselsdato found for person with ident $ident")
            return Result.failure(
                PdlException("Found person without fødselsdato, see teamlog for ident")
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
