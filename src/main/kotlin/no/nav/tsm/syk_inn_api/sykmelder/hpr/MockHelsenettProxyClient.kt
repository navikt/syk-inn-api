package no.nav.tsm.syk_inn_api.sykmelder.hpr

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Component

@Profile("local", "test")
@Component
class MockHelsenettProxyClient : IHelsenettProxyClient {
    init {
        println("MockHelsenettProxyClient initialized")
    }

    override fun getSykmelderByHpr(behandlerHpr: String, callId: String): Result<HprSykmelder> {
        if(behandlerHpr == "brokenHpr") {
            return Result.failure(
                IllegalStateException("MockHelsenettProxyClient: Simulated failure for brokenHpr")
            )
        }
        if(behandlerHpr == "hprButHasBrokenFnrAndNoGodkjenninger") {
            return Result.success(
                HprSykmelder(
                    godkjenninger = emptyList(),
                    fnr = "brokenFnr",
                    hprNummer = "hprButHasBrokenFnrAndNoGodkjenninger",
                    fornavn = "James",
                    mellomnavn = "007",
                    etternavn = "Bond"
                )
            )
        }
        if(behandlerHpr == "hprButFnrIsSuspended") {
            return Result.success(
                HprSykmelder(
                    godkjenninger = emptyList(),
                    fnr = "suspendedFnr",
                    hprNummer = "hprButFnrIsSuspended",
                    fornavn = "James",
                    mellomnavn = "007",
                    etternavn = "Bond"
                )
            )
        }
        return Result.success(
            HprSykmelder(
                godkjenninger =
                    listOf(
                        HprGodkjenning(
                            helsepersonellkategori = HprKode(aktiv = true, oid = 0, verdi = "LE"),
                            autorisasjon = HprKode(aktiv = true, oid = 7704, verdi = "1"),
                            tillegskompetanse = null
                        )
                    ),
                fnr = "09099012345",
                hprNummer = "123456789",
                fornavn = "James",
                mellomnavn = "007",
                etternavn = "Bond"
            )
        )
    }

    override fun getSykmelderByFnr(behandlerFnr: String, callId: String): Result<HprSykmelder> {
        if(behandlerFnr == "brokenFnr") {
            return Result.failure(
                IllegalStateException("MockHelsenettProxyClient: Simulated failure for brokenFnr")
            )
        }
        return Result.success(
            HprSykmelder(
                godkjenninger =
                    listOf(
                        HprGodkjenning(
                            helsepersonellkategori = HprKode(aktiv = true, oid = 0, verdi = "LE"),
                            autorisasjon = HprKode(aktiv = true, oid = 7704, verdi = "1"),
                            tillegskompetanse = null
                        )
                    ),
                fnr = "09099012345",
                hprNummer = "123456789",
                fornavn = "James",
                mellomnavn = "007",
                etternavn = "Bond"
            )
        )
    }
}
