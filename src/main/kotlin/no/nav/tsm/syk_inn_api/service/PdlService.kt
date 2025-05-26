package no.nav.tsm.syk_inn_api.service

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.client.IPdlClient

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
        val result = pdlClient.getPerson(fnr)

        return result.fold(
            {
                it.foedselsdato
                    ?: throw IllegalStateException("Fødselsdato is null for fnr=$fnr")
            },
        ) {
            secureLog.error("Error while fetching birth date for fnr=$fnr", it)
            throw it // should we handle the flow differently ? or use the throw here?
        }
    }

    fun getPdlPerson(fnr: String): PdlPerson {
        val result = pdlClient.getPerson(fnr)

        return result.fold({ it }) {
            secureLog.error("Error while fetching person info for fnr=$fnr", it)
            throw it // should we handle the flow differently ? or use the throw here?
        }
    }
}
