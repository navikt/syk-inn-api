package no.nav.tsm.syk_inn_api.pdf

import java.time.LocalDate
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentAktivitet
import no.nav.tsm.syk_inn_api.utils.toReadableDatePeriod

object SykmeldingDocumentUtils {
    fun formatReadablePeriode(aktivitet: List<SykmeldingDocumentAktivitet>): String {
        val firstFom =
            aktivitet.minOfOrNull { it.fom }?.let { LocalDate.parse(it) }
                ?: throw IllegalStateException("Aktivity-less sykmelding, shouldn't happen")
        val lastTom =
            aktivitet.maxOfOrNull { it.tom }?.let { LocalDate.parse(it) }
                ?: throw IllegalStateException("Aktivity-less sykmelding, shouldn't happen")

        return toReadableDatePeriod(firstFom, lastTom)
    }
}
