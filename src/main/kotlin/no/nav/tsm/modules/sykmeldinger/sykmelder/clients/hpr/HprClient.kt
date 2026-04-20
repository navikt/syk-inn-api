package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr

import arrow.core.Either

sealed interface HprClient {

    enum class HprErrors {
        NotFound,
        UnknownError,
    }

    suspend fun getSykmelderByHpr(behandlerHpr: String): Either<HprErrors, SykmelderMedHpr>

    suspend fun getSykmelderByIdent(behandlerIdent: String): Either<HprErrors, SykmelderMedHpr>
}
