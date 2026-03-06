package modules.sykmelder

import core.logger
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import modules.sykmelder.clients.btsys.BtsysClient
import modules.sykmelder.clients.hpr.HprClient

class SykmelderService(private val btsys: BtsysClient, private val helsenettProxy: HprClient) {
    val logger = logger()

    @WithSpan
    suspend fun sykmelder(hpr: String): Sykmelder.Enkel? {
        val sykmelder =
            helsenettProxy.getSykmelderByHpr(behandlerHpr = hpr)?.let {
                Sykmelder.Enkel(hpr = it.hprNummer, ident = it.ident)
            }

        return sykmelder
    }

    @WithSpan
    suspend fun sykmelderByIdent(ident: String): Sykmelder.Enkel? {
        val sykmelder =
            helsenettProxy.getSykmelderByIdent(behandlerIdent = ident)?.let {
                Sykmelder.Enkel(hpr = it.hprNummer, ident = it.ident)
            }

        return sykmelder
    }

    @WithSpan
    suspend fun sykmelderMedSuspensjon(
        hpr: String,
        signaturDato: LocalDate,
    ): Sykmelder.MedSuspensjon? {
        val sykmelder = this.sykmelder(hpr) ?: return null
        val suspendert = btsys.isSuspendert(sykmelder.ident, oppslagsdato = signaturDato)

        return sykmelder.toMedSuspensjon(suspendert)
    }
}
