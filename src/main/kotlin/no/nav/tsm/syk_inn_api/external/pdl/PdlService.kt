package no.nav.tsm.syk_inn_api.external.pdl

import no.nav.tsm.syk_inn_api.common.Result
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: IPdlClient,
) {
    private val logger = LoggerFactory.getLogger(PdlService::class.java)
    private val secureLog: Logger = LoggerFactory.getLogger("securelog")

    fun getPdlPerson(fnr: String): PdlPerson {
        return when (val result = pdlClient.getPerson(fnr)) {
            is Result.Success -> {
                result.data
            }
            is Result.Failure -> {
                secureLog.error("Error while fetching person info for fnr=$fnr", result.error)
                logger.error("Error while fetching person info from PDL, check secure logs")
                throw result.error // should we handle the flow differently ? or use the throw here?
            }
        }
    }
}
