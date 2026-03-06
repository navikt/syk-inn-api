package modules.sykmelder.clients.btsys

import java.time.LocalDate

class BtsysLocalClient : BtsysClient {
    override suspend fun isSuspendert(sykmelderIdent: String, oppslagsdato: LocalDate): Boolean {
        return sykmelderIdent == "12345678910"
    }
}
