package no.nav.tsm.syk_inn_api.utils

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

fun toReadableDate(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.forLanguageTag("nb"))
    return date.format(formatter)
}

fun toReadableDateNoYear(date: LocalDate): String {
    val formatter = DateTimeFormatter.ofPattern("d. MMMM", Locale.forLanguageTag("nb"))
    return date.format(formatter)
}

fun toReadableDatePeriod(fom: LocalDate, tom: LocalDate): String {
    return when {
        fom.isEqual(tom) -> toReadableDate(fom)
        fom.month == tom.month && fom.year == tom.year ->
            "${fom.dayOfMonth}. - ${toReadableDate(tom)}"
        fom.year == tom.year -> "${toReadableDateNoYear(fom)} - ${toReadableDate(tom)}"
        else -> "${toReadableDate(fom)} - ${toReadableDate(tom)}"
    }
}
