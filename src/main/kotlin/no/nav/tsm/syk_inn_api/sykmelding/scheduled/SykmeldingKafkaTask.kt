package no.nav.tsm.syk_inn_api.sykmelding.scheduled

import java.time.OffsetDateTime
import java.time.ZoneOffset.UTC
import java.util.concurrent.TimeUnit
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingProducer
import no.nav.tsm.syk_inn_api.sykmelding.persistence.NextSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.persistence.Status
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingRepository
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingStatusRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class SykmeldingKafkaTask(
    private val sykmeldingProducer: SykmeldingProducer,
    private val sykmeldingStatusRepository: SykmeldingStatusRepository,
    private val sykmeldingRepo: SykmeldingRepository,
    @Value($$"${jobs.sykmelding.resetTimeoutSeconds}") private val resetTimeoutSeconds: Long,
) {
    private val log = LoggerFactory.getLogger(SykmeldingKafkaTask::class.java)

    @Scheduled(
        initialDelayString = $$"${jobs.sykmelding.initialDelaySeconds}",
        fixedDelayString = $$"${jobs.sykmelding.fixedDelaySeconds}",
        timeUnit = TimeUnit.SECONDS,
    )
    fun sendPendingSykmeldingToKafka() {
        val resetCount =
            sykmeldingStatusRepository.resetSykmeldingSending(
                OffsetDateTime.now(UTC).minusSeconds(resetTimeoutSeconds)
            )
        if (resetCount > 0) {
            log.info("Reset $resetCount sykmeldinger for sending")
        }

        do {
            val nextSykmelding = sendNextSykmelding()
            if (nextSykmelding != null) {
                log.info("Did send sykmelding with id: $nextSykmelding")
            }
        } while (nextSykmelding != null)
    }

    private fun sendNextSykmelding(): NextSykmelding? {
        val nextSykmelding = sykmeldingStatusRepository.getNextSykmelding()
        if (nextSykmelding != null) {
            try {
                val sykmelding =
                    sykmeldingRepo.getSykmeldingDbBySykmeldingId(
                        nextSykmelding.sykmeldingId.toString(),
                    )
                requireNotNull(sykmelding) { "Sykmelding with id=${nextSykmelding} not found" }
                sykmeldingProducer.send(sykmelding, nextSykmelding.source)
                sykmeldingStatusRepository.updateStatus(
                    nextSykmelding.sykmeldingId,
                    Status.SENT,
                )
            } catch (ex: Exception) {
                log.error(
                    "Failed to send sykmelding with id=${nextSykmelding.sykmeldingId}",
                    ex,
                )
                sykmeldingStatusRepository.updateStatus(
                    nextSykmelding.sykmeldingId,
                    Status.FAILED,
                )
            }
        }
        return nextSykmelding
    }
}
