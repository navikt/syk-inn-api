package modules.sykmeldinger.sykmelder.clients.hpr

import modules.sykmeldinger.sykmelder.SykmelderMedHpr

class HprLocalClient : HprClient {

    override suspend fun getSykmelderByHpr(behandlerHpr: String): SykmelderMedHpr {
        return SykmelderMedHpr(
            ident = "12345678910",
            hprNummer = "007007999",
            fornavn = "Karlo",
            mellomnavn = "Roberto",
            etternavn = "Sykmeldersen",
        )
    }

    override suspend fun getSykmelderByIdent(behandlerIdent: String): SykmelderMedHpr {
        return SykmelderMedHpr(
            ident = "12345678910",
            hprNummer = "007007999",
            fornavn = "Karlo",
            mellomnavn = "Roberto",
            etternavn = "Sykmeldersen",
        )
    }
}
