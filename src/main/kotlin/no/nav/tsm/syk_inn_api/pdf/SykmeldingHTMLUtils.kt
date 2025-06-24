package no.nav.tsm.syk_inn_api.pdf

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentYrkesskade
import no.nav.tsm.syk_inn_api.utils.toReadableDate
import no.nav.tsm.syk_inn_api.utils.toReadableDatePeriod

object SykmeldingHTMLUtils {
    fun formatReadablePeriode(aktivitet: SykmeldingDocumentAktivitet): String {
        val timePeriodLabel =
            toReadableDatePeriod(LocalDate.parse(aktivitet.fom), LocalDate.parse(aktivitet.tom))

        val aktivitetsLabel =
            when (aktivitet) {
                is SykmeldingDocumentAktivitet.IkkeMulig -> "100% sykmeldt, aktivitet ikke mulig"
                is SykmeldingDocumentAktivitet.Gradert ->
                    "${aktivitet.grad}% sykmeldt" +
                        if (aktivitet.reisetilskudd) " med reisetilskudd" else ""
                is SykmeldingDocumentAktivitet.Avventende -> TODO()
                is SykmeldingDocumentAktivitet.Behandlingsdager -> TODO()
                is SykmeldingDocumentAktivitet.Reisetilskudd -> TODO()
            }

        return "$timePeriodLabel - $aktivitetsLabel"
    }

    fun yrkesskadeText(yrkesskade: SykmeldingDocumentYrkesskade?): String? {
        if (yrkesskade == null) return null

        val skadedatoTekst =
            if (yrkesskade.skadedato != null)
                ", skadedato: ${
            toReadableDate(yrkesskade.skadedato)
        }"
            else ""

        return "Sykmeldingen er relatert til yrkesskade${skadedatoTekst}"
    }

    fun svangerskapsrelatertText(svangerskapsrelatert: Boolean): String? {
        if (!svangerskapsrelatert) return null

        return "Sykmeldingen er svangerskapsrelatert"
    }
}
