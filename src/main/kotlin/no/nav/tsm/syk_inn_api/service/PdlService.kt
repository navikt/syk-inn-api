package no.nav.tsm.syk_inn_api.service

import no.nav.tsm.syk_inn_api.client.PdlClient
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class PdlService(
    private val pdlClient : PdlClient,
) {

    fun getFodselsdato(fnr: String): LocalDate {
        //TODO implement using pdl cache thingy
        return pdlClient.getFodselsdato(fnr)
        // pdlClient.getFodselsdato(fnr)
        return LocalDate.now().minusDays(1)
    }
}
