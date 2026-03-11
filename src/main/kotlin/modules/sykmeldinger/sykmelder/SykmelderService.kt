package modules.sykmeldinger.sykmelder

import core.logger
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.time.LocalDate
import modules.sykmeldinger.sykmelder.clients.btsys.BtsysClient
import modules.sykmeldinger.sykmelder.clients.hpr.HprClient

class SykmelderService(private val btsys: BtsysClient, private val helsenettProxy: HprClient) {
    private val logger = logger()

    @WithSpan
    suspend fun byHpr(hpr: String, oppslagsdato: LocalDate): Sykmelder {
        val sykmelderMedHpr =
            helsenettProxy.getSykmelderByHpr(behandlerHpr = hpr)
                ?: return Sykmelder.FinnesIkke(hpr = hpr)

        val suspendert =
            btsys.isSuspendert(sykmelderIdent = sykmelderMedHpr.ident, oppslagsdato = oppslagsdato)
                ?: return Sykmelder.UtenSuspensjon(hpr = hpr, ident = sykmelderMedHpr.ident)
        // TODO vi må håndtere exceptions som blir kasta frå klientane. Skal vi catche det her eller
        // lenger opp? vi må hard stoppe. Må prate om korleis vi vil handtere det.

        return Sykmelder.MedSuspensjon(
            hpr = hpr,
            ident = sykmelderMedHpr.ident,
            suspendert = suspendert,
        )
    }
}
