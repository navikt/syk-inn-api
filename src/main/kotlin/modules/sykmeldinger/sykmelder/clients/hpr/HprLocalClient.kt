package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import no.nav.tsm.core.logger

class HprLocalClient : HprClient {
    private val logger = logger()

    override suspend fun getSykmelderByHpr(behandlerHpr: String): SykmelderMedHpr {
        if (behandlerHpr == "brokenHpr") {
            logger.info("HprMock: Got brokenHpr, mocking failure.")

            throw IllegalStateException("MockHelsenettProxyClient: Simulated failure for brokenHpr")
        }

        if (behandlerHpr == "hprButHasBrokenFnrAndNoGodkjenninger") {
            logger.info(
                "HprMock: Got hprButHasBrokenFnrAndNoGodkjenninger, mocking broken FNR with no godkjenninger."
            )

            return SykmelderMedHpr(
                ident = "brokenFnr",
                hprNummer = "hprButHasBrokenFnrAndNoGodkjenninger",
                godkjenninger = emptyList(),
            )
        }

        if (behandlerHpr == "hprButFnrIsSuspended") {
            logger.info(
                "HprMock: Got hprButFnrIsSuspended, returning as normal and letting btsys mock handle the rest"
            )

            return SykmelderMedHpr(
                ident = "suspendertFnr",
                hprNummer = "hprButFnrIsSuspended",
                godkjenninger = aktivLegeGodnkjenninger,
            )
        }

        logger.info("HprMock: Got $behandlerHpr, mocking normal response.")
        return SykmelderMedHpr(
            ident = "09099012345",
            hprNummer = behandlerHpr,
            godkjenninger = aktivLegeGodnkjenninger,
        )
    }

    override suspend fun getSykmelderByIdent(behandlerIdent: String): SykmelderMedHpr {
        if (behandlerIdent == "brokenFnr") {
            logger.info("HprMock: Got brokenFnr, mocking failure.")
            throw IllegalStateException("MockHelsenettProxyClient: Simulated failure for brokenFnr")
        }

        logger.info("HprMock: Got $behandlerIdent, mocking normal response.")
        return SykmelderMedHpr(
            ident = behandlerIdent,
            hprNummer = "123456789",
            godkjenninger = emptyList(),
        )
    }
}

private val aktivLegeGodnkjenninger: List<SykmelderGodkjenning> =
    listOf(
        SykmelderGodkjenning(
            helsepersonellkategori = SykmelderKode(aktiv = true, oid = 0, verdi = "LE"),
            autorisasjon = SykmelderKode(aktiv = true, oid = 7704, verdi = "1"),
            tillegskompetanse = null,
        )
    )
