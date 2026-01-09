package no.nav.tsm.syk_inn_api.sykmelding.scheduled

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.verify
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.syk_inn_api.sykmelding.kafka.producer.SykmeldingProducer
import no.nav.tsm.syk_inn_api.sykmelding.persistence.Status
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingRepository
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingStatusRepository
import no.nav.tsm.syk_inn_api.test.FullIntegrationTest
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class SykmeldingKafkaTaskTest() : FullIntegrationTest() {

    @MockkBean(relaxed = true) lateinit var sykmeldingProducer: SykmeldingProducer

    @SpykBean lateinit var sykmeldingStatusRepository: SykmeldingStatusRepository

    @MockkBean(relaxed = true) lateinit var sykmeldingRepo: SykmeldingRepository

    @Test
    fun testEmptyDb() {
        val sykmeldingKafkaTask =
            SykmeldingKafkaTask(
                sykmeldingStatusRepository = sykmeldingStatusRepository,
                sykmeldingProducer = sykmeldingProducer,
                sykmeldingRepo = sykmeldingRepo,
                resetTimeoutSeconds = 1800L,
            )
        sykmeldingKafkaTask.sendPendingSykmeldingToKafka()

        verify(exactly = 1) { sykmeldingStatusRepository.resetSykmeldingSending(any()) }
        verify(exactly = 1) { sykmeldingStatusRepository.getNextSykmelding() }
        verify(exactly = 0) { sykmeldingRepo.getSykmeldingDbBySykmeldingId(any()) }
        verify(exactly = 0) { sykmeldingProducer.send(any(), any()) }
    }

    @Test
    fun testDbWithSykmelding() {
        val sykmeldingKafkaTask =
            SykmeldingKafkaTask(
                sykmeldingStatusRepository = sykmeldingStatusRepository,
                sykmeldingProducer = sykmeldingProducer,
                sykmeldingRepo = sykmeldingRepo,
                resetTimeoutSeconds = 1800L,
            )
        val uuid = UUID.randomUUID()
        val nowTimestamp = OffsetDateTime.now()
        val mottatt = nowTimestamp.minusMinutes(10)
        val sendt = nowTimestamp.minusMinutes(5)
        sykmeldingStatusRepository.insert(uuid, mottatt, sendt, "source")
        sykmeldingKafkaTask.sendPendingSykmeldingToKafka()

        verify(exactly = 1) { sykmeldingStatusRepository.resetSykmeldingSending(any()) }
        verify(exactly = 2) { sykmeldingStatusRepository.getNextSykmelding() }
        verify(exactly = 1) { sykmeldingRepo.getSykmeldingDbBySykmeldingId(any()) }
        verify(exactly = 1) { sykmeldingProducer.send(any(), any()) }

        val status = sykmeldingStatusRepository.getSykmeldingStatus(uuid)
        assertEquals(Status.SENT, status?.status)
    }

    @Test
    fun `should not send sykmelding with sendTimestamp ahead of time`() {
        val sykmeldingKafkaTask =
            SykmeldingKafkaTask(
                sykmeldingStatusRepository = sykmeldingStatusRepository,
                sykmeldingProducer = sykmeldingProducer,
                sykmeldingRepo = sykmeldingRepo,
                resetTimeoutSeconds = 1800L,
            )
        val uuid = UUID.randomUUID()
        val nowTimestamp = OffsetDateTime.now()
        val mottatt = nowTimestamp.minusMinutes(10)
        val sendt = nowTimestamp.plusMinutes(5)
        sykmeldingStatusRepository.insert(uuid, mottatt, sendt, "source")
        sykmeldingKafkaTask.sendPendingSykmeldingToKafka()

        verify(exactly = 1) { sykmeldingStatusRepository.resetSykmeldingSending(any()) }
        verify(exactly = 1) { sykmeldingStatusRepository.getNextSykmelding() }
        verify(exactly = 0) { sykmeldingRepo.getSykmeldingDbBySykmeldingId(any()) }
        verify(exactly = 0) { sykmeldingProducer.send(any(), any()) }

        val status = sykmeldingStatusRepository.getSykmeldingStatus(uuid)
        assertEquals(Status.PENDING, status?.status)
    }

    @Test
    fun `should update status if kafka fails`() {
        val sykmeldingKafkaTask =
            SykmeldingKafkaTask(
                sykmeldingStatusRepository = sykmeldingStatusRepository,
                sykmeldingProducer = sykmeldingProducer,
                sykmeldingRepo = sykmeldingRepo,
                resetTimeoutSeconds = 1800L,
            )
        val uuid = UUID.randomUUID()
        val nowTimestamp = OffsetDateTime.now()
        val mottatt = nowTimestamp.minusMinutes(10)
        val sendt = nowTimestamp.minusMinutes(5)
        sykmeldingStatusRepository.insert(uuid, mottatt, sendt, "source")

        every { sykmeldingProducer.send(any(), any()) } throws RuntimeException("Failed to send")

        sykmeldingKafkaTask.sendPendingSykmeldingToKafka()

        verify(exactly = 1) { sykmeldingStatusRepository.resetSykmeldingSending(any()) }
        verify(exactly = 2) { sykmeldingStatusRepository.getNextSykmelding() }
        verify(exactly = 1) { sykmeldingRepo.getSykmeldingDbBySykmeldingId(any()) }
        verify(exactly = 1) { sykmeldingProducer.send(any(), any()) }

        val status = sykmeldingStatusRepository.getSykmeldingStatus(uuid)
        assertEquals(Status.FAILED, status?.status)
    }

    @Test
    fun `should reset failed sykmelding and try again`() {
        val sykmeldingKafkaTask =
            SykmeldingKafkaTask(
                sykmeldingStatusRepository = sykmeldingStatusRepository,
                sykmeldingProducer = sykmeldingProducer,
                sykmeldingRepo = sykmeldingRepo,
                resetTimeoutSeconds = 1800L,
            )
        val uuid = UUID.randomUUID()
        val nowTimestamp = OffsetDateTime.now()
        val mottatt = nowTimestamp.minusMinutes(10)
        val sendt = nowTimestamp.minusMinutes(5)
        sykmeldingStatusRepository.insert(uuid, mottatt, sendt, "source")

        every { sykmeldingProducer.send(any(), any()) } throws RuntimeException("Failed to send")

        sykmeldingKafkaTask.sendPendingSykmeldingToKafka()

        verify(exactly = 1) { sykmeldingStatusRepository.resetSykmeldingSending(any()) }
        verify(exactly = 2) { sykmeldingStatusRepository.getNextSykmelding() }
        verify(exactly = 1) { sykmeldingRepo.getSykmeldingDbBySykmeldingId(any()) }
        verify(exactly = 1) { sykmeldingProducer.send(any(), any()) }

        val status = sykmeldingStatusRepository.getSykmeldingStatus(uuid)
        assertEquals(Status.FAILED, status?.status)

        every { sykmeldingProducer.send(any(), any()) } returns Unit

        sykmeldingStatusRepository.resetSykmeldingSending(nowTimestamp.plusHours(1))
        sykmeldingKafkaTask.sendPendingSykmeldingToKafka()
        verify(exactly = 2) { sykmeldingProducer.send(any(), any()) }

        val updatedStatus = sykmeldingStatusRepository.getSykmeldingStatus(uuid)
        assertEquals(Status.SENT, updatedStatus?.status)
    }

    @Test
    fun `should reset failed and sending sykmelding and try again`() {
        var sykmeldingKafkaTask =
            SykmeldingKafkaTask(
                sykmeldingStatusRepository = sykmeldingStatusRepository,
                sykmeldingProducer = sykmeldingProducer,
                sykmeldingRepo = sykmeldingRepo,
                resetTimeoutSeconds = 1800L,
            )
        val failedUUID = UUID.randomUUID()
        val sendingUUID = UUID.randomUUID()
        val sentUUID = UUID.randomUUID()
        val pendingUUID = UUID.randomUUID()
        val nowTimestamp = OffsetDateTime.now()
        val mottatt = nowTimestamp.minusMinutes(50)
        val sendt = nowTimestamp.minusMinutes(45)
        sykmeldingStatusRepository.insert(failedUUID, mottatt, sendt, "source")
        sykmeldingStatusRepository.insert(sendingUUID, mottatt, sendt, "source")
        sykmeldingStatusRepository.insert(sentUUID, mottatt, sendt, "source")
        sykmeldingStatusRepository.insert(pendingUUID, mottatt, sendt, "source")

        sykmeldingStatusRepository.updateStatus(sendingUUID, Status.SENDING)
        sykmeldingStatusRepository.updateStatus(sentUUID, Status.SENT)
        sykmeldingStatusRepository.updateStatus(pendingUUID, Status.PENDING)
        sykmeldingStatusRepository.updateStatus(failedUUID, Status.FAILED)

        sykmeldingKafkaTask.sendPendingSykmeldingToKafka()

        verify(exactly = 1) { sykmeldingStatusRepository.resetSykmeldingSending(any()) }
        verify(exactly = 2) { sykmeldingStatusRepository.getNextSykmelding() }
        verify(exactly = 1) { sykmeldingRepo.getSykmeldingDbBySykmeldingId(any()) }
        verify(exactly = 1) { sykmeldingProducer.send(any(), any()) }

        val status = sykmeldingStatusRepository.getSykmeldingStatus(pendingUUID)
        assertEquals(Status.SENT, status?.status)

        sykmeldingKafkaTask =
            SykmeldingKafkaTask(
                sykmeldingStatusRepository = sykmeldingStatusRepository,
                sykmeldingProducer = sykmeldingProducer,
                sykmeldingRepo = sykmeldingRepo,
                resetTimeoutSeconds = -2, // should reset immediately
            )

        sykmeldingKafkaTask.sendPendingSykmeldingToKafka()
        verify(exactly = 2) { sykmeldingStatusRepository.resetSykmeldingSending(any()) }
        verify(exactly = 5) { sykmeldingStatusRepository.getNextSykmelding() }
        verify(exactly = 3) { sykmeldingRepo.getSykmeldingDbBySykmeldingId(any()) }
        verify(exactly = 3) { sykmeldingProducer.send(any(), any()) }
    }
}
