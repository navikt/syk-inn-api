package no.nav.tsm.modules.sykmeldinger.sykmelder

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys.BtsysClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.HprClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.SykmelderMedHpr

class SykmelderService(private val btsys: BtsysClient, private val helsenettProxy: HprClient) {
    sealed interface SykmelderErrors {
        val details: String

        class SuspendertError(override val details: String) : SykmelderErrors

        class HprError(override val details: String) : SykmelderErrors
    }

    @WithSpan
    suspend fun byHpr(hpr: String, oppslagsdato: LocalDate): Either<SykmelderErrors, Sykmelder> =
        either {
            val sykmelderMedHpr: SykmelderMedHpr =
                helsenettProxy
                    .getSykmelderByHpr(behandlerHpr = hpr)
                    .mapLeft {
                        when (it) {
                            HprClient.HprErrors.NotFound -> return@either Sykmelder.FinnesIkke
                            HprClient.HprErrors.UnknownError ->
                                SykmelderErrors.HprError("Unknown HPR error")
                        }
                    }
                    .bind()

            val suspendert: Boolean =
                btsys
                    .isSuspendert(
                        sykmelderIdent = sykmelderMedHpr.ident,
                        oppslagsdato = oppslagsdato,
                    )
                    .mapLeft {
                        when (it) {
                            BtsysClient.SuspendertErrors.NotFound ->
                                SykmelderErrors.SuspendertError("User not found")
                            BtsysClient.SuspendertErrors.UnknownError ->
                                SykmelderErrors.SuspendertError("Unknown error")
                        }
                    }
                    .bind()

            return Sykmelder.MedSuspensjon(
                    hpr = hpr,
                    navn = sykmelderMedHpr.navn,
                    ident = sykmelderMedHpr.ident,
                    suspendert = suspendert,
                    godkjenninger = sykmelderMedHpr.godkjenninger,
                )
                .right()
        }

    suspend fun byIdent(
        ident: String,
        oppslagsdato: LocalDate,
    ): Either<SykmelderErrors, Sykmelder> = either {
        val sykmelderMedHpr =
            helsenettProxy
                .getSykmelderByIdent(ident)
                .mapLeft {
                    when (it) {
                        HprClient.HprErrors.NotFound -> return@either Sykmelder.FinnesIkke
                        HprClient.HprErrors.UnknownError ->
                            SykmelderErrors.HprError("Unknown HPR error")
                    }
                }
                .bind()

        val suspendert: Boolean =
            btsys
                .isSuspendert(sykmelderIdent = sykmelderMedHpr.ident, oppslagsdato = oppslagsdato)
                .mapLeft {
                    when (it) {
                        BtsysClient.SuspendertErrors.NotFound ->
                            SykmelderErrors.SuspendertError("User not found")
                        BtsysClient.SuspendertErrors.UnknownError ->
                            SykmelderErrors.SuspendertError("Unknown error")
                    }
                }
                .bind()

        return Sykmelder.MedSuspensjon(
                hpr = sykmelderMedHpr.hprNummer,
                navn = sykmelderMedHpr.navn,
                ident = sykmelderMedHpr.ident,
                suspendert = suspendert,
                godkjenninger = sykmelderMedHpr.godkjenninger,
            )
            .right()
    }
}
