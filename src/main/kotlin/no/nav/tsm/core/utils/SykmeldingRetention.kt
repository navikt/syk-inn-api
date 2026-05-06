import java.time.LocalDate
import no.nav.tsm.core.Environment

fun Environment.sykmeldingCutoffDate(): LocalDate {
    require(sykmeldingConfig.retention.inWholeDays >= 1) {
        "Retention to must have at least 1 day, are you trying to test?"
    }

    return LocalDate.now().minusDays(sykmeldingConfig.retention.inWholeDays)
}
