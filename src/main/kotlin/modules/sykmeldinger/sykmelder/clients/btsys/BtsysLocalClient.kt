package modules.sykmeldinger.sykmelder.clients.btsys

import java.time.LocalDate

class BtsysLocalClient : BtsysClient {
    override suspend fun isSuspendert(sykmelderIdent: String, oppslagsdato: LocalDate): Boolean {
        if (sykmelderIdent == "brokenFnr") {
            throw IllegalStateException("MockBtsysClient: Simulated failure for brokenFnr")
        }

        if (sykmelderIdent == "suspendertFnr") {
            return true
        }

        return false
    }
}
