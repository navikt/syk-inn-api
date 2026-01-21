package no.nav.tsm.syk_inn_api.pdf

import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentYrkesskade
import no.nav.tsm.syk_inn_api.utils.toReadableDate
import no.nav.tsm.syk_inn_api.utils.toReadableDatePeriod
import no.nav.tsm.sykmelding.input.core.model.AnnenFravarsgrunn

object SykmeldingHTMLUtils {
    fun formatReadablePeriode(aktivitet: SykmeldingDocumentAktivitet): List<String> {
        val periode = mutableListOf<String>()
        val timePeriodLabel = toReadableDatePeriod(aktivitet.fom, aktivitet.tom)

        val aktivitetsLabel =
            when (aktivitet) {
                is SykmeldingDocumentAktivitet.IkkeMulig -> "100% sykmeldt, aktivitet ikke mulig"
                is SykmeldingDocumentAktivitet.Gradert -> "${aktivitet.grad}% sykmeldt" +
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
                        aktivitet.arbeidsrelatertArsak.arbeidsrelaterteArsaker.joinToString(", "),
                    )
                    if (aktivitet.arbeidsrelatertArsak.annenArbeidsrelatertArsak != null) {
                        periode.add(
                            "Annen arbeidsrelatert årsak: " +
                                aktivitet.arbeidsrelatertArsak.annenArbeidsrelatertArsak,
                        )
                    }
                }
            }

            else -> {}
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

    fun annenFravarsgrunnToText(grunn: AnnenFravarsgrunn): String =
        when (grunn) {
            AnnenFravarsgrunn.ABORT -> "Pasienten er arbeidsufør som følge av svangerskapsavbrudd"
            AnnenFravarsgrunn.BEHANDLING_FORHINDRER_ARBEID ->
                "Pasienten er under behandling som gjør det nødvendig med fravær fra arbeid (ikke enkeltstående behandlingsdager)"
            AnnenFravarsgrunn.ARBEIDSRETTET_TILTAK -> "Pasienten deltar på et arbeidsrettet tiltak"
            AnnenFravarsgrunn.BEHANDLING_STERILISERING ->
                "Pasienten er arbeidsufør som følge av behandling i forbindelse med sterilisering"
            AnnenFravarsgrunn.DONOR -> "Pasienten er donor eller er under vurdering som donor"
            AnnenFravarsgrunn.GODKJENT_HELSEINSTITUSJON ->
                "Pasienten er innlagt i en godkjent helseinstitusjon"
            AnnenFravarsgrunn.MOTTAR_TILSKUDD_GRUNNET_HELSETILSTAND ->
                "Pasienten mottar tilskott til opplæringstiltak på grunn av sykdom, skade eller lyte"
            AnnenFravarsgrunn.NODVENDIG_KONTROLLUNDENRSOKELSE ->
                "Pasienten er til nødvendig kontrollundersøkelse som krever minst 24 timers fravær"
            AnnenFravarsgrunn.SMITTEFARE ->
                "Pasienten har forbud mot å arbeide på grunn av smittefare"
            AnnenFravarsgrunn.UFOR_GRUNNET_BARNLOSHET ->
                "Pasienten er arbeidsufør som følge av behandling for barnløshet"
        }
}
