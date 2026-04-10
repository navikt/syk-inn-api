package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import arrow.core.Either
import arrow.core.right
import no.nav.tsm.core.common.name.SimpleNavn
import no.nav.tsm.core.logger

class HprLocalClient : HprClient {
    private val logger = logger()

    override suspend fun getSykmelderByHpr(
        behandlerHpr: String
    ): Either<HprClient.HprErrors, SykmelderMedHpr> {
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
                    godkjenninger = aktivLegeGodnkjenninger,
                )
                .right()
        }

        logger.info("HprMock: Got $behandlerHpr, mocking normal response.")
        return SykmelderMedHpr(
                ident = "09099012345",
                hprNummer = behandlerHpr,
                navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                godkjenninger = aktivLegeGodnkjenninger,
            )
            .right()
    }

    override suspend fun getSykmelderByIdent(
        behandlerIdent: String
    ): Either<HprClient.HprErrors, SykmelderMedHpr> {
        if (behandlerIdent == "brokenFnr") {
            logger.info("HprMock: Got brokenFnr, mocking failure.")
            throw IllegalStateException("MockHelsenettProxyClient: Simulated failure for brokenFnr")
        }

        logger.info("HprMock: Got $behandlerIdent, mocking normal response.")
        return SykmelderMedHpr(
                ident = behandlerIdent,
                hprNummer = "123456789",
                navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                godkjenninger = emptyList(),
            )
            .right()
    }
}

private val aktivLegeGodnkjenninger: List<SykmelderGodkjenning> =
    listOf(
        SykmelderGodkjenning(
            helsepersonellkategori = SykmelderKodeverk(aktiv = true, oid = 0, verdi = "LE"),
            autorisasjon = SykmelderKodeverk(aktiv = true, oid = 7704, verdi = "1"),
            tillegskompetanse = null,
        )
    )
