package no.nav.tsm.syk_inn_api.sykmelding.rules.juridiskvurdering

import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.concurrent.TimeUnit
import no.nav.tsm.syk_inn_api.sykmelding.rules.JuridiskHenvisningRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SendJuridiskhenvisningerTask(
    private val juridiskHenvisningProducer: JuridiskHenvisningProducer,
    private val juridiskHenvisningRepository: JuridiskHenvisningRepository,
    @Value($$"${jobs.juridisk.resetTimeoutSeconds}") private val resetTimeoutSeconds: Long,
) {

    private val log = LoggerFactory.getLogger(SendJuridiskhenvisningerTask::class.java)

    @Scheduled(
        initialDelayString = $$"${jobs.juridisk.initialDelaySeconds}",
        fixedDelayString = $$"${jobs.juridisk.fixedDelaySeconds}",
        timeUnit = TimeUnit.SECONDS,
    )
    fun sendJuridiskhenvisninger() {
        val resetCount =
            juridiskHenvisningRepository.resetHangingJobs(
                OffsetDateTime.now(UTC).minusSeconds(resetTimeoutSeconds)
            )

        if (resetCount > 0) log.info("Reset $resetCount juridiskhenvisninger for sending")

        do {
            val nextJuridiskhenvisning = juridiskHenvisningRepository.getNextToSend()
            if (nextJuridiskhenvisning != null) {
                try {
                    juridiskHenvisningProducer.send(
                        nextJuridiskhenvisning.sykmeldingId.toString(),
                        nextJuridiskhenvisning.juridiskhenvisning
                    )
                    juridiskHenvisningRepository.markAsSent(nextJuridiskhenvisning)
                    log.info("Sent juridiskhenvisning for sykmeldingId=${nextJuridiskhenvisning.sykmeldingId}")
                } catch (ex: Exception) {
                    log.error(
                        "Failed to send juridiskhenvisning for sykmeldingId=${nextJuridiskhenvisning.sykmeldingId}",
                        ex
                    )
                    juridiskHenvisningRepository.markAsFailed(nextJuridiskhenvisning)
                }
            }
        } while (nextJuridiskhenvisning != null)
    }
}
