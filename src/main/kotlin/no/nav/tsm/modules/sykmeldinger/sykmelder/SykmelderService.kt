package no.nav.tsm.modules.sykmeldinger.sykmelder

import arrow.core.Either
import arrow.core.raise.either
import arrow.core.right
import io.ktor.server.plugins.di.annotations.Named
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import no.nav.tsm.core.logger
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys.BtsysClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.HprClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.SykmelderMedHpr

class SykmelderService(
    private val btsys: BtsysClient,
    private val helsenettProxy: HprClient,
    @Named("HprRestClient") private val hprRestClient: HprClient,
) {
    private val logger = logger()

    enum class SykmelderErrors {
        HprUnknownError,
        SuspendertNotFound,
        SuspendertUnknownError,
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
                            HprClient.HprErrors.UnknownError -> SykmelderErrors.HprUnknownError
                        }
                    }
                    .bind()

            try {
                logger.info("Trying out HprRestClient for HPR $hpr!! :))")
                hprRestClient
                    .getSykmelderByHpr(hpr)
                    .fold(
                        ifLeft = { logger.error("HprRestClient(HPR) failed with error: $it") },
                        ifRight = {
                            val isSame = sykmelderMedHpr.let { old ->
                                old.hprNummer == it.hprNummer ||
                                    old.ident == it.ident ||
                                    old.navn.fornavn == it.navn.fornavn ||
                                    old.navn.etternavn == it.navn.etternavn ||
                                    old.godkjenninger.size == it.godkjenninger.size
                            }
                            if (!isSame) {
                                logger.info(
                                    "HprRestClient(HPR) succeeded but there was a diff!! HPR: ${sykmelderMedHpr.hprNummer}"
                                )
                            }
                        },
                    )
            } catch (e: Exception) {
                logger.error(
                    "HprRestClient(HPR) threw an exception for HPR $hpr, ignoring and continuing",
                    e,
                )
            }

            val suspendert: Boolean =
                btsys
                    .isSuspendert(
                        sykmelderIdent = sykmelderMedHpr.ident,
                        oppslagsdato = oppslagsdato,
                    )
                    .mapLeft {
                        when (it) {
                            BtsysClient.SuspendertErrors.NotFound ->
                                SykmelderErrors.SuspendertNotFound

                            BtsysClient.SuspendertErrors.UnknownError ->
                                SykmelderErrors.SuspendertUnknownError
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
                        HprClient.HprErrors.UnknownError -> SykmelderErrors.HprUnknownError
                    }
                }
                .bind()

        try {
            logger.info("Trying out HprRestClient for ident!! :))")
            hprRestClient
                .getSykmelderByHpr(ident)
                .fold(
                    ifLeft = { logger.error("HprRestClient(Ident) failed with error: $it") },
                    ifRight = {
                        val isSame = sykmelderMedHpr.let { old ->
                            old.hprNummer == it.hprNummer ||
                                old.ident == it.ident ||
                                old.navn.fornavn == it.navn.fornavn ||
                                old.navn.etternavn == it.navn.etternavn ||
                                old.godkjenninger.size == it.godkjenninger.size
                        }
                        if (!isSame) {
                            logger.info(
                                "HprRestClient(Ident) succeeded but there was a diff!! HPR: ${sykmelderMedHpr.hprNummer}"
                            )
                        }
                    },
                )
        } catch (e: Exception) {
            logger.error("HprRestClient(Ident) threw an exception, ignoring and continuing", e)
        }

        val suspendert: Boolean =
            btsys
                .isSuspendert(sykmelderIdent = sykmelderMedHpr.ident, oppslagsdato = oppslagsdato)
                .mapLeft {
                    when (it) {
                        BtsysClient.SuspendertErrors.NotFound -> SykmelderErrors.SuspendertNotFound
                        BtsysClient.SuspendertErrors.UnknownError ->
                            SykmelderErrors.SuspendertUnknownError
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
