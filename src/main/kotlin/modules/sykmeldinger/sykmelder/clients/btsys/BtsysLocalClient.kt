package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys

import java.time.LocalDate
import no.nav.tsm.core.logger

class BtsysLocalClient : BtsysClient {
    private val logger = logger()

    override suspend fun isSuspendert(sykmelderIdent: String, oppslagsdato: LocalDate): Boolean {
        if (sykmelderIdent == "brokenFnr") {
            logger.info("BtsysMock: Got brokenFnr, mocking failure.")
            throw IllegalStateException("MockBtsysClient: Simulated failure for brokenFnr")
        }

        if (sykmelderIdent == "suspendertFnr") {
            logger.info("BtsysMock: Got suspendertFnr, mocked user is suspended")
            return true
        }

        logger.info("BtsysMock: Got $sykmelderIdent, mocking not suspended")
        return false
    }
}
