package no.nav.tsm.syk_inn_api.service

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.client.PdlClient
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: PdlClient,
) {
    private val logger = LoggerFactory.getLogger(PdlService::class.java)
    private val secureLog: Logger = LoggerFactory.getLogger("securelog")

    fun getFodselsdato(fnr: String): LocalDate {
        return when (val result = pdlClient.getFodselsdato(fnr)) {
            is PdlClient.Result.Success -> result.data
            is PdlClient.Result.Failure -> {
                secureLog.error("Error while fetching birth date for fnr=$fnr", result.error)
                throw result.error
            }
        }
    }
}
