package modules.sykmelder.clients.hpr

import modules.sykmelder.SykmelderMedHpr

class LocalHprClient : HprClient {

    override suspend fun getSykmelderByHpr(behandlerHpr: String, callId: String): SykmelderMedHpr? {
        return SykmelderMedHpr(
            ident = "12345678910",
            hprNummer = "007007999",
            fornavn = "Karlo",
            mellomnavn = "Roberto",
            etternavn = "Sykmeldersen",
        )
    }

    override suspend fun getSykmelderByIdent(
        behandlerIdent: String,
        callId: String,
    ): SykmelderMedHpr? {
        return SykmelderMedHpr(
            ident = "12345678910",
            hprNummer = "007007999",
            fornavn = "Karlo",
            mellomnavn = "Roberto",
            etternavn = "Sykmeldersen",
        )
    }
}
