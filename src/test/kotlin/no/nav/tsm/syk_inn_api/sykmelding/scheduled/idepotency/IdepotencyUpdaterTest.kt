package no.nav.tsm.syk_inn_api.sykmelding.scheduled.idepotency

import java.time.LocalDate
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingRepository
import no.nav.tsm.syk_inn_api.sykmelding.persistence.createTestSykmeldingDb
import no.nav.tsm.syk_inn_api.test.FullIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@DataJpaTest(showSql = false)
@Import(IdepotencyUpdater::class)
class IdepotencyUpdaterTest : FullIntegrationTest() {

    @Autowired lateinit var idepotencyUpdater: IdepotencyUpdater

    @Autowired lateinit var sykmeldingRepository: SykmeldingRepository

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `should update idempotency key`() {
        (0 until 2_000).forEach {
            val sykmelding =
                createTestSykmeldingDb(UUID.randomUUID().toString(), "12345678912", LocalDate.now())
                    .copy(idempotencyKey = null)
            sykmeldingRepository.save(sykmelding)
        }

        (0 until 2).forEach {
            val updated = idepotencyUpdater.updateSykmeldinger()
            assertEquals(1000, updated)
        }

        val noUpdateds = idepotencyUpdater.updateSykmeldinger()
        assertEquals(0, noUpdateds)
    }
}
