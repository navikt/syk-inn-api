package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler

import arrow.core.Either
import arrow.core.right
import no.nav.tsm.ktor.core.SimpleNavn
import no.nav.tsm.ktor.logger

class TsmBehandlerLocalClient : TsmBehandlerClient {
    private val logger = logger()

    override suspend fun getSykmelderByHpr(
        behandlerHpr: String
    ): Either<TsmBehandlerClient.BehandlerErrors, SykmelderMedHpr> {
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
                    navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                    godkjenninger = emptyList(),
                    suspendert = false,
                )
                .right()
        }

        if (behandlerHpr == "hprButFnrIsSuspended") {
            logger.info(
                "HprMock: Got hprButFnrIsSuspended, returning as normal and letting btsys mock handle the rest"
            )

            return SykmelderMedHpr(
                    ident = "suspendertFnr",
                    hprNummer = "hprButFnrIsSuspended",
                    navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                    godkjenninger = aktivLegeGodkjenninger,
                    suspendert = true,
                )
                .right()
        }

        logger.info("HprMock: Got $behandlerHpr, mocking normal response.")
        return SykmelderMedHpr(
                ident = "09099012345",
                hprNummer = behandlerHpr,
                navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                godkjenninger = aktivLegeGodkjenninger,
                suspendert = false,
            )
            .right()
    }

    override suspend fun getSykmelderByIdent(
        fnr: String
    ): Either<TsmBehandlerClient.BehandlerErrors, SykmelderMedHpr> {
        if (fnr == "brokenFnr") {
            logger.info("HprMock: Got brokenFnr, mocking failure.")
            throw IllegalStateException("MockHelsenettProxyClient: Simulated failure for brokenFnr")
        }
        var suspendert = false
        if (fnr == "suspendertFnr") {
            logger.info("BtsysMock: Got suspendertFnr, mocked user is suspended")
            suspendert = true
        }

        logger.info("HprMock: Got $fnr, mocking normal response.")
        return SykmelderMedHpr(
                ident = fnr,
                hprNummer = "123456789",
                navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                godkjenninger = emptyList(),
                suspendert = suspendert,
            )
            .right()
    }
}

private val aktivLegeGodkjenninger: List<SykmelderGodkjenning> =
    listOf(
        SykmelderGodkjenning(
            helsepersonellkategori = SykmelderKode(aktiv = true, oid = 0, verdi = "LE"),
            autorisasjon = SykmelderKode(aktiv = true, oid = 7704, verdi = "1"),
            tillegskompetanse = null,
        )
    )
