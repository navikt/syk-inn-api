package no.nav.tsm.syk_inn_api.sykmelding.persistence

import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.tsm.syk_inn_api.sykmelding.getTestSykmelding
import no.nav.tsm.syk_inn_api.test.FullIntegrationTest
import org.junit.jupiter.api.assertNull
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.context.annotation.Import
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional

@DataJpaTest
@Import(SykmeldingPersistenceService::class, SykmeldingStatusRepository::class)
class SykmeldingPersistenceServiceTest : FullIntegrationTest() {

    @Autowired lateinit var sykmeldingPersistenceService: SykInnPersistence

    @Autowired lateinit var sykmeldingStatusRepository: SykmeldingStatusRepository

    @Autowired lateinit var sykmeldingRepository: SykmeldingRepository

    @Test
    fun `test insert OK`() {
        val sykmeldingDb = db(getTestSykmelding())

        val saved = sykmeldingPersistenceService.saveNewSykmelding(sykmeldingDb, null)

        assertEquals(sykmeldingDb, saved)
        val savedStatus =
            sykmeldingStatusRepository.getSykmeldingStatus(
                UUID.fromString(sykmeldingDb.sykmeldingId)
            )
        assertEquals(Status.PENDING, savedStatus?.status)
    }

    @Test
    fun `test insert failed if sykmeldingID already exists`() {
        val persistedSykmelding = getTestSykmelding()
        val sykmeldingDb = db(persistedSykmelding)
        sykmeldingPersistenceService.saveNewSykmelding(sykmeldingDb, null)
        assertThrows<DataIntegrityViolationException> {
            sykmeldingPersistenceService.saveNewSykmelding(sykmeldingDb, null)
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `test insert failed if sykmeldingID already exists in status`() {
        val persistedSykmelding = getTestSykmelding()
        val sykmeldingDb = db(persistedSykmelding)
        sykmeldingStatusRepository.insert(
            UUID.fromString(sykmeldingDb.sykmeldingId),
            sykmeldingDb.mottatt,
            sykmeldingDb.mottatt,
            "MY (FHIR)"
        )
        assertThrows<DataIntegrityViolationException> {
            sykmeldingPersistenceService.saveNewSykmelding(sykmeldingDb, null)
        }
        val sykmelding =
            sykmeldingRepository.getSykmeldingDbBySykmeldingId(sykmeldingDb.sykmeldingId)
        assertNull(sykmelding)
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `test insert failed if idempotencyKey already exists`() {
        val persistedSykmelding = getTestSykmelding()
        val sykmeldingDb = db(persistedSykmelding)
        sykmeldingPersistenceService.saveNewSykmelding(sykmeldingDb, null)
        val duplicateSykmelding = sykmeldingDb.copy(sykmeldingId = UUID.randomUUID().toString())
        assertThrows<DataIntegrityViolationException> {
            sykmeldingPersistenceService.saveNewSykmelding(duplicateSykmelding, null)
        }
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    fun `test insert ok if new sykmeldingID and new IdempotencyKey`() {
        val persistedSykmelding = getTestSykmelding()
        val sykmeldingDb = db(persistedSykmelding)
        val savedSykmelding = sykmeldingPersistenceService.saveNewSykmelding(sykmeldingDb, null)
        val newSykmelding =
            sykmeldingDb.copy(
                sykmeldingId = UUID.randomUUID().toString(),
                idempotencyKey = UUID.randomUUID()
            )
        val newSavedSykmeldign = sykmeldingPersistenceService.saveNewSykmelding(newSykmelding, null)
        assertEquals(savedSykmelding, sykmeldingDb)
        assertEquals(newSykmelding, newSavedSykmeldign)
    }

    private fun db(persistedSykmelding: PersistedSykmelding): SykmeldingDb =
        SykmeldingDb(
            sykmeldingId = persistedSykmelding.sykmeldingId,
            mottatt = OffsetDateTime.now(),
            pasientIdent = persistedSykmelding.pasient.ident,
            sykmelderHpr = persistedSykmelding.sykmelder.hprNummer,
            sykmelding = persistedSykmelding,
            legekontorOrgnr = null,
            legekontorTlf = null,
            fom = persistedSykmelding.aktivitet.minOf { it.fom },
            tom = persistedSykmelding.aktivitet.maxOf { it.tom },
            idempotencyKey = UUID.randomUUID(),
            validationResult =
                PersistedValidationResult(PersistedRuleType.OK, OffsetDateTime.now(), emptyList())
        )
}
