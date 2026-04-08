package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import no.nav.tsm.core.db.runFlywayMigrations
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable
import no.nav.tsm.utils.WithPostgresql
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class SykmeldingProducerRepoTest : WithPostgresql() {

    companion object {
        init {
            runFlywayMigrations(postgres.jdbcUrl, postgres.username, postgres.password)
            Database.connect(
                url = postgres.jdbcUrl,
                user = postgres.username,
                password = postgres.password,
            )
        }
    }

    private val repo = SykmeldingProducerRepo()

    @BeforeTest
    fun cleanup() {
        transaction { SykmeldingStatusTable.deleteAll() }
    }

    @Test
    fun `getNext returns null when no pending items exist`() {
        val result = repo.getNext()
        assertNull(result)
    }

    @Test
    fun `getNext returns pending item and updates status to SENDING`() {
        val sykmeldingId = UUID.randomUUID()
        insertSykmeldingStatus(sykmeldingId, SykmeldingStatusStatus.PENDING)

        val result = repo.getNext()

        assertNotNull(result)
        assertEquals(sykmeldingId, result.sykmeldingId)
        assertEquals(SykmeldingStatusStatus.SENDING, result.status)
    }

    @Test
    fun `getNext picks oldest by send_timestamp first`() {
        val older = UUID.randomUUID()
        val newer = UUID.randomUUID()
        insertSykmeldingStatus(
            older,
            SykmeldingStatusStatus.PENDING,
            sendTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2),
        )
        insertSykmeldingStatus(
            newer,
            SykmeldingStatusStatus.PENDING,
            sendTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
        )

        val result = repo.getNext()

        assertNotNull(result)
        assertEquals(older, result.sykmeldingId)
    }

    @Test
    fun `getNext ignores non-PENDING items`() {
        insertSykmeldingStatus(UUID.randomUUID(), SykmeldingStatusStatus.SENDING)
        insertSykmeldingStatus(UUID.randomUUID(), SykmeldingStatusStatus.SENT)
        insertSykmeldingStatus(UUID.randomUUID(), SykmeldingStatusStatus.FAILED)

        val result = repo.getNext()
        assertNull(result)
    }

    @Test
    fun `getNext called twice returns different items`() {
        val first = UUID.randomUUID()
        val second = UUID.randomUUID()
        insertSykmeldingStatus(
            first,
            SykmeldingStatusStatus.PENDING,
            sendTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2),
        )
        insertSykmeldingStatus(
            second,
            SykmeldingStatusStatus.PENDING,
            sendTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
        )

        val result1 = repo.getNext()
        val result2 = repo.getNext()

        assertNotNull(result1)
        assertNotNull(result2)
        assertEquals(first, result1.sykmeldingId)
        assertEquals(second, result2.sykmeldingId)
    }

    @Test
    fun `resetHangingJobs resets SENDING and FAILED to PENDING`() {
        val sending = UUID.randomUUID()
        val failed = UUID.randomUUID()
        val oldEventTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2)
        insertSykmeldingStatus(
            sending,
            SykmeldingStatusStatus.SENDING,
            eventTimestamp = oldEventTimestamp,
        )
        insertSykmeldingStatus(
            failed,
            SykmeldingStatusStatus.FAILED,
            eventTimestamp = oldEventTimestamp,
        )

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(2, count)
        transaction {
            SykmeldingStatusTable.selectAll().forEach { row ->
                assertEquals(SykmeldingStatusStatus.PENDING.name, row[SykmeldingStatusTable.status])
            }
        }
    }

    @Test
    fun `resetHangingJobs does not reset recent items`() {
        val recent = UUID.randomUUID()
        insertSykmeldingStatus(recent, SykmeldingStatusStatus.SENDING)

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(0, count)
    }

    @Test
    fun `resetHangingJobs does not reset PENDING or SENT items`() {
        val oldEventTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2)
        insertSykmeldingStatus(
            UUID.randomUUID(),
            SykmeldingStatusStatus.PENDING,
            eventTimestamp = oldEventTimestamp,
        )
        insertSykmeldingStatus(
            UUID.randomUUID(),
            SykmeldingStatusStatus.SENT,
            eventTimestamp = oldEventTimestamp,
        )

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(0, count)
    }

    private fun insertSykmeldingStatus(
        sykmeldingId: UUID,
        status: SykmeldingStatusStatus,
        eventTimestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
        sendTimestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    ) {
        transaction {
            SykmeldingStatusTable.insert {
                it[SykmeldingStatusTable.sykmeldingId] = sykmeldingId
                it[SykmeldingStatusTable.status] = status.name
                it[SykmeldingStatusTable.eventTimestamp] = eventTimestamp
                it[SykmeldingStatusTable.mottattTimestamp] = OffsetDateTime.now(ZoneOffset.UTC)
                it[SykmeldingStatusTable.sendTimestamp] = sendTimestamp
                it[SykmeldingStatusTable.sourceSystem] = "test"
            }
        }
    }
}
