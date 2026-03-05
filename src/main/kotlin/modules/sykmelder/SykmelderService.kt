package modules.sykmelder

import core.logger
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import modules.sykmelder.clients.btsys.BtsysClient

class SykmelderService(private val btsys: BtsysClient) {
    val logger = logger()

    @WithSpan
    suspend fun sykmelder(hpr: String): Sykmelder.Enkel? {
        TODO("Stub")
    }

    @WithSpan
    suspend fun sykmelderByIdent(ident: String): Sykmelder.Enkel {
        TODO("Just a stub")
    }

    @WithSpan
    suspend fun sykmelderMedSuspensjon(
        hpr: String,
        signaturDato: LocalDate,
    ): Sykmelder.MedSuspensjon? {
        val sykmelder = this.sykmelder(hpr)
        if (sykmelder == null) return null

        val suspendert = TODO("Implement Btsys Client requset")
        return sykmelder.toMedSuspensjon(suspendert)
    }
}
