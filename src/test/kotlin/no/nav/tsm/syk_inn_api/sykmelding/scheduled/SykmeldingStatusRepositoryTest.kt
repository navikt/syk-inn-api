package no.nav.tsm.syk_inn_api.sykmelding.scheduled

import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.syk_inn_api.sykmelding.persistence.NextSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.persistence.Status
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingStatusRepository
import no.nav.tsm.syk_inn_api.test.FullIntegrationTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.jdbc.test.autoconfigure.JdbcTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@JdbcTest
@Import(SykmeldingStatusRepository::class)
class SykmeldingStatusRepositoryTest() : FullIntegrationTest() {

    @Autowired lateinit var sykmeldingStausRepository: SykmeldingStatusRepository

    @Test
    fun insertStatus() {
        val sykmeldingId = UUID.randomUUID()
        val mottattTimestamp = OffsetDateTime.now()
        val inserted =
            sykmeldingStausRepository.insert(
                sykmeldingId,
                mottattTimestamp,
                mottattTimestamp,
                "source"
            )

        assertThat(inserted).isEqualTo(1)
    }

    @Test
    fun getNextSykmelding() {
        val sykmeldingId = sykmeldingStausRepository.getNextSykmelding()
        assertThat(sykmeldingId).isEqualTo(null)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun insertAndGetNextSykmelding() {
        val id = UUID.randomUUID()
        val mottattTimestamp = OffsetDateTime.now().minusMinutes(10)
        val inserted =
            sykmeldingStausRepository.insert(id, mottattTimestamp, mottattTimestamp, "source")
        assertThat(inserted).isEqualTo(1)

        val sykmeldingId = sykmeldingStausRepository.getNextSykmelding()
        assertThat(sykmeldingId).isEqualTo(NextSykmelding(id, "source"))

        val nextSykmelding = sykmeldingStausRepository.getNextSykmelding()
        assertThat(nextSykmelding).isEqualTo(null)

        val updated = sykmeldingStausRepository.updateStatus(id, Status.FAILED)
        assertThat(updated).isEqualTo(1)

        val failedSykmelding = sykmeldingStausRepository.getNextSykmelding()
        assertThat(failedSykmelding).isEqualTo(null)

        var sykmeldingStatus = sykmeldingStausRepository.getSykmeldingStatus(id)
        assertThat(sykmeldingStatus?.status).isEqualTo(Status.FAILED)

        val reset = sykmeldingStausRepository.resetSykmeldingSending(mottattTimestamp.plusHours(1L))
        assertThat(reset).isEqualTo(1)

        sykmeldingStatus = sykmeldingStausRepository.getSykmeldingStatus(id)
        assertThat(sykmeldingStatus?.status).isEqualTo(Status.PENDING)

        sykmeldingStausRepository.updateStatus(id, Status.SENDING)
        sykmeldingStatus = sykmeldingStausRepository.getSykmeldingStatus(id)
        assertThat(sykmeldingStatus?.status).isEqualTo(Status.SENDING)

        sykmeldingStausRepository.resetSykmeldingSending(mottattTimestamp.plusHours(2L))
        val resetSykmelding = sykmeldingStausRepository.getNextSykmelding()
        assertThat(resetSykmelding).isEqualTo(NextSykmelding(id, "source"))

        sykmeldingStausRepository.updateStatus(id, Status.SENT)

        assertEquals(sykmeldingStausRepository.getNextSykmelding(), null)
        assertEquals(
            sykmeldingStausRepository.resetSykmeldingSending(mottattTimestamp.plusHours(3)),
            0
        )
    }

    @Test
    fun `should not insert duplicate sykmelding status`() {
        val sykmeldingId = UUID.randomUUID()
        val mottattTimestamp = OffsetDateTime.now()
        sykmeldingStausRepository.insert(sykmeldingId, mottattTimestamp, mottattTimestamp, "source")
        assertThrows<DataIntegrityViolationException> {
            sykmeldingStausRepository.insert(
                sykmeldingId,
                mottattTimestamp,
                mottattTimestamp,
                "source"
            )
        }
    }
}
