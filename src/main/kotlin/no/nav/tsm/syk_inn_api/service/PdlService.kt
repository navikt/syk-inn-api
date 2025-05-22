package no.nav.tsm.syk_inn_api.service

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.client.IPdlClient
import no.nav.tsm.syk_inn_api.client.Result
import no.nav.tsm.syk_inn_api.model.PdlPerson
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: IPdlClient,
) {
    private val logger = LoggerFactory.getLogger(PdlService::class.java)
    private val secureLog: Logger = LoggerFactory.getLogger("securelog")

    // TODO safe to delete - do we want it ?
    fun getFodselsdato(fnr: String): LocalDate {
        return when (val result = pdlClient.getPerson(fnr)) {
            is Result.Success -> {
                result.data.foedselsdato
                    ?: throw IllegalStateException("Fødselsdato is null for fnr=$fnr")
            }
            is Result.Failure -> {
                secureLog.error("Error while fetching birth date for fnr=$fnr", result.error)
                throw result.error // should we handle the flow differently ? or use the throw here?
            }
        }
    }

    fun getPdlPerson(fnr: String): PdlPerson {
        return when (val result = pdlClient.getPerson(fnr)) {
            is Result.Success -> {
                result.data
            }
            is Result.Failure -> {
                secureLog.error("Error while fetching person info for fnr=$fnr", result.error)
                throw result.error // should we handle the flow differently ? or use the throw here?
            }
        }
    }
}
