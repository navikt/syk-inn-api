package no.nav.tsm.syk_inn_api.sykmelding.rules.juridiskvurdering

import com.ninjasquad.springmockk.MockkBean
import com.ninjasquad.springmockk.SpykBean
import io.mockk.every
import io.mockk.verify
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test
import no.nav.tsm.syk_inn_api.sykmelding.rules.JuridiskHenvisningRepository
import no.nav.tsm.syk_inn_api.sykmelding.rules.JuridiskVurderingResult
import no.nav.tsm.syk_inn_api.test.FullIntegrationTest
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest

@DataJpaTest
class SendJuridiskhenvisningerTaskTest : FullIntegrationTest() {

    @MockkBean(relaxed = true) lateinit var juridiskHenvisningProducer: JuridiskHenvisningProducer

    @SpykBean lateinit var juridiskHenvisningRepository: JuridiskHenvisningRepository

    @Test
    fun testSendJuridiskhenvisningerTask() {
        val task =
            SendJuridiskhenvisningerTask(
                juridiskHenvisningProducer,
                juridiskHenvisningRepository,
                1800L
            )
        val uuid = UUID.randomUUID()
        juridiskHenvisningRepository.insert(
            uuid,
            OffsetDateTime.now(),
            JuridiskVurderingResult(emptyList())
        )

        task.sendJuridiskhenvisninger()

        verify(exactly = 1) { juridiskHenvisningRepository.resetHangingJobs(any()) }
        verify(exactly = 2) { juridiskHenvisningRepository.getNextToSend() }
        verify(exactly = 1) { juridiskHenvisningProducer.send(any(), any()) }
        verify(exactly = 1) { juridiskHenvisningRepository.markAsSent(any()) }
        verify(exactly = 0) { juridiskHenvisningRepository.markAsFailed(any()) }
    }

    @Test
    fun testSendJuridiskhenvisningerTaskFails() {
        val task =
            SendJuridiskhenvisningerTask(
                juridiskHenvisningProducer,
                juridiskHenvisningRepository,
                1800L
            )
        val uuid = UUID.randomUUID()
        juridiskHenvisningRepository.insert(
            uuid,
            OffsetDateTime.now(),
            JuridiskVurderingResult(emptyList())
        )

        every { juridiskHenvisningProducer.send(any(), any()) } throws RuntimeException("Failed")

        task.sendJuridiskhenvisninger()
        verify(exactly = 1) { juridiskHenvisningRepository.resetHangingJobs(any()) }
        verify(exactly = 2) { juridiskHenvisningRepository.getNextToSend() }
        verify(exactly = 1) { juridiskHenvisningProducer.send(any(), any()) }
        verify(exactly = 0) { juridiskHenvisningRepository.markAsSent(any()) }
        verify(exactly = 1) { juridiskHenvisningRepository.markAsFailed(any()) }
    }
}
