package no.nav.tsm.syk_inn_api.sykmelding.scheduled.idepotency

import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class UpdateIdepotencyKeyJob(private val idepotencyUpdater: IdepotencyUpdater) {

    private val log = logger()

    @Scheduled(initialDelay = 10_000L)
    fun updateIdepotencyKey() {
        var totalUpdates = 0
        do {
            val updated = idepotencyUpdater.updateSykmeldinger()
            totalUpdates += updated
            if (totalUpdates % 100_000 == 0)
                log.info("Updated $totalUpdates sykmeldinger with idempotency key")
        } while (updated > 0)

        log.info("Finished updating idempotency keys, updates: $totalUpdates")
    }
}
