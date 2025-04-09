package no.nav.tsm.syk_inn_api.service

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.client.IPdlClient
import no.nav.tsm.syk_inn_api.client.Result
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class PdlService(
    private val pdlClient: IPdlClient,
) {
    private val logger = LoggerFactory.getLogger(PdlService::class.java)
    private val secureLog: Logger = LoggerFactory.getLogger("securelog")

    fun getFodselsdato(fnr: String): LocalDate {
        return when (val result = pdlClient.getFodselsdato(fnr)) {
            is Result.Success -> result.data
            is Result.Failure -> {
                secureLog.error("Error while fetching birth date for fnr=$fnr", result.error)
                throw result.error
            }
        }
    }
}
