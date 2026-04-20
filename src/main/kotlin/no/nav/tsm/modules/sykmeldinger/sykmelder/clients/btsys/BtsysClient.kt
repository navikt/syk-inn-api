package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys

import arrow.core.Either
import java.time.LocalDate

sealed interface BtsysClient {

    enum class SuspendertErrors {
        NotFound,
        UnknownError,
    }

    suspend fun isSuspendert(
        sykmelderIdent: String,
        oppslagsdato: LocalDate,
    ): Either<SuspendertErrors, Boolean>
}
