package no.nav.tsm.modules.sykmeldinger.sykmelder

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.opentelemetry.instrumentation.annotations.WithSpan
import no.nav.tsm.ktor.logger
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler.TsmBehandlerClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.SykmelderMedHpr

class SykmelderService(private val tsmBehandlerClient: TsmBehandlerClient) {
    private val logger = logger()

    enum class SykmelderErrors {
        HprUnknownError,
        SuspendertNotFound,
        SuspendertUnknownError,
    }

    @WithSpan
    suspend fun byHpr(hpr: String): Either<SykmelderErrors, Sykmelder> = either {
        val sykmelderMedHpr: SykmelderMedHpr =
            tsmBehandlerClient
                .getSykmelderByHpr(behandlerHpr = hpr)
                .mapLeft {
                    when (it) {
                        TsmBehandlerClient.BehandlerErrors.NotFound ->
                            return@either Sykmelder.FinnesIkke
                        TsmBehandlerClient.BehandlerErrors.Unknown ->
                            SykmelderErrors.HprUnknownError
                    }
                }
                .bind()

        return Sykmelder.MedSuspensjon(
                hpr = hpr,
                navn = sykmelderMedHpr.navn,
                ident = sykmelderMedHpr.ident,
                suspendert = sykmelderMedHpr.suspendert,
                godkjenninger = sykmelderMedHpr.godkjenninger,
            )
            .right()
    }

    suspend fun byIdent(ident: String): Either<SykmelderErrors, Sykmelder> = either {
        val behandler = tsmBehandlerClient.getSykmelderByIdent(ident)
        val sykmelderMedHpr =
            behandler
                .mapLeft {
                    when (it) {
                        TsmBehandlerClient.BehandlerErrors.NotFound ->
                            return@either Sykmelder.FinnesIkke
                        TsmBehandlerClient.BehandlerErrors.Unknown ->
                            SykmelderErrors.HprUnknownError
                    }
                }
                .bind()

        return Sykmelder.MedSuspensjon(
                hpr = sykmelderMedHpr.hprNummer,
                navn = sykmelderMedHpr.navn,
                ident = sykmelderMedHpr.ident,
                suspendert = sykmelderMedHpr.suspendert,
                godkjenninger = sykmelderMedHpr.godkjenninger,
            )
            .right()
    }
}
