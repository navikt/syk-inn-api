package no.nav.tsm.syk_inn_api.sykmelder

import arrow.core.flatMap
import java.time.LocalDate
import no.nav.tsm.syk_inn_api.common.Navn
import no.nav.tsm.syk_inn_api.sykmelder.btsys.IBtsysClient
import no.nav.tsm.syk_inn_api.sykmelder.hpr.IHelsenettProxyClient
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.stereotype.Service

@Service
class SykmelderService(
    private val btsysClient: IBtsysClient,
    private val helsenettProxyClient: IHelsenettProxyClient,
) {
    val logger = logger()

    fun sykmelder(hpr: String, callId: String): Result<Sykmelder.Enkel> {
        val sykmelder =
            helsenettProxyClient.getSykmelderByHpr(hpr, callId).mapCatching {
                Sykmelder.Enkel(
                    hpr = it.hprNummer ?: hpr,
                    navn =
                        it.fornavn?.let { fornavn ->
                            Navn(
                                fornavn = fornavn,
                                mellomnavn = it.mellomnavn,
                                etternavn = it.etternavn
                                        ?: throw IllegalStateException(
                                            "Has fornavn, but no etternavn. Seems weird."
                                        ),
                            )
                        },
                    ident = it.fnr,
                    godkjenninger = it.godkjenninger,
                )
            }

        return sykmelder
    }

    fun sykmelderByFnr(fnr: String, callId: String): Result<Sykmelder.Enkel> {
        val sykmelder =
            helsenettProxyClient.getSykmelderByFnr(fnr, callId).mapCatching {
                Sykmelder.Enkel(
                    hpr = it.hprNummer
                            ?: throw java.lang.IllegalStateException(
                                "Sykmelder looked up with fnr is missing hpr number "
                            ),
                    navn =
                        it.fornavn?.let { fornavn ->
                            Navn(
                                fornavn = fornavn,
                                mellomnavn = it.mellomnavn,
                                etternavn = it.etternavn
                                        ?: throw IllegalStateException(
                                            "Has fornavn, but no etternavn. Seems weird."
                                        ),
                            )
                        },
                    ident = it.fnr,
                    godkjenninger = it.godkjenninger,
                )
            }

        return sykmelder
    }

    fun sykmelderMedSuspensjon(
        hpr: String,
        signaturDato: LocalDate,
        callId: String
    ): Result<Sykmelder.MedSuspensjon> =
        sykmelder(hpr, callId).flatMap { sykmelder ->
            btsysClient
                .checkSuspensionStatus(sykmelder.ident, signaturDato)
                .map { it.suspendert }
                .map { suspendert -> sykmelder.toMedSuspensjon(suspendert) }
        }
}
