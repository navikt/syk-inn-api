package no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler

import arrow.core.Either
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.SykmelderMedHpr

sealed interface BehandlerQuery {
    enum class QueryType {
        HPR,
        FNR,
    }

    val id: String
    val type: QueryType

    data class FnrQuery(override val id: String) : BehandlerQuery {
        override val type = QueryType.FNR
    }

    data class HprQuery(override val id: String) : BehandlerQuery {
        override val type = QueryType.HPR
    }
}

sealed interface TsmBehandlerClient {
    enum class BehandlerErrors {
        NotFound,
        Unknown,
    }

    suspend fun getSykmelderByHpr(behandlerHpr: String): Either<BehandlerErrors, SykmelderMedHpr>

    suspend fun getSykmelderByIdent(fnr: String): Either<BehandlerErrors, SykmelderMedHpr>
}
