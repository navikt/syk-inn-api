package no.nav.tsm.sykinnapi.service.tsmpdl

import no.nav.tsm.sykinnapi.client.tsmpdl.TsmPdlClient
import no.nav.tsm.sykinnapi.modell.tsmpdl.PdlPerson
import org.springframework.stereotype.Service

@Service
class TsmPdlService(
    private val tsmPdlClient: TsmPdlClient,
) {
    fun getPdlPerson(
        ident: String,
    ): PdlPerson {

        val pdlPerson = tsmPdlClient.getPdlPerson(ident)

        return pdlPerson
    }
}
