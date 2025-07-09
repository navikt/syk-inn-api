package no.nav.tsm.syk_inn_api.pdf

import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentYrkesskade
import no.nav.tsm.syk_inn_api.utils.toReadableDate
import no.nav.tsm.syk_inn_api.utils.toReadableDatePeriod

object SykmeldingHTMLUtils {
    fun formatReadablePeriode(aktivitet: SykmeldingDocumentAktivitet): List<String> {
        val periode = mutableListOf<String>()
        val timePeriodLabel = toReadableDatePeriod(aktivitet.fom, aktivitet.tom)

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

        periode.add("$timePeriodLabel - $aktivitetsLabel")

        when (aktivitet) {
            is SykmeldingDocumentAktivitet.IkkeMulig -> {
                if (aktivitet.medisinskArsak.isMedisinskArsak) {
                    periode.add("Medisinske årsaker forhindrer aktivetet")
                }
                if (aktivitet.arbeidsrelatertArsak.isArbeidsrelatertArsak) {
                    periode.add("Arbeidsrelaterte årsaker forhindrer aktivitet")
                    periode.add(
                        aktivitet.arbeidsrelatertArsak.arbeidsrelaterteArsaker.joinToString(", ")
                    )
                    if (aktivitet.arbeidsrelatertArsak.annenArbeidsrelatertArsak != null) {
                        periode.add(
                            "Annen arbeidsrelatert årsak: " +
                                aktivitet.arbeidsrelatertArsak.annenArbeidsrelatertArsak
                        )
                    }
                }
            }
            else -> null
        }

        return periode
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
