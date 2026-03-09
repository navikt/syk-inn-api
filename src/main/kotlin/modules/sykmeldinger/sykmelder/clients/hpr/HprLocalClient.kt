package modules.sykmeldinger.sykmelder.clients.hpr

import modules.sykmeldinger.sykmelder.SykmelderMedHpr

class HprLocalClient : HprClient {

    override suspend fun getSykmelderByHpr(behandlerHpr: String): SykmelderMedHpr {
        if (behandlerHpr == "brokenHpr") {
            throw IllegalStateException("MockHelsenettProxyClient: Simulated failure for brokenHpr")
        }

        if (behandlerHpr == "hprButHasBrokenFnrAndNoGodkjenninger") {
            return SykmelderMedHpr(
                ident = "brokenFnr",
                hprNummer = "hprButHasBrokenFnrAndNoGodkjenninger",
                fornavn = "James",
                mellomnavn = "007",
                etternavn = "Bond",
            )
        }

        if (behandlerHpr == "hprButFnrIsSuspended") {
            return SykmelderMedHpr(
                ident = "suspendertFnr",
                hprNummer = "hprButFnrIsSuspended",
                fornavn = "James",
                mellomnavn = "007",
                etternavn = "Bond",
            )
        }

        return SykmelderMedHpr(
            ident = "09099012345",
            hprNummer = "123456789",
            fornavn = "James",
            mellomnavn = "007",
            etternavn = "Bond",
        )
    }

    override suspend fun getSykmelderByIdent(behandlerIdent: String): SykmelderMedHpr {
        if (behandlerIdent == "brokenFnr") {
            throw IllegalStateException("MockHelsenettProxyClient: Simulated failure for brokenFnr")
        }

        return SykmelderMedHpr(
            ident = "09099012345",
            hprNummer = "123456789",
            fornavn = "James",
            mellomnavn = "007",
            etternavn = "Bond",
        )
    }
}
