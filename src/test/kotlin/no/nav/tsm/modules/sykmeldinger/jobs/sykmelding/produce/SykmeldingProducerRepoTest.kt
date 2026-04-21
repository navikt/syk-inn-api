package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.produce

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.*
import kotlin.test.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable
import no.nav.tsm.utils.WithPostgresql
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll

class SykmeldingProducerRepoTest : WithPostgresql() {

    companion object {
        init {
            runMigrations(true)
            connect()
        }
    }

    private val repo = SykmeldingProducerRepo()

    @BeforeTest fun cleanup(): Unit = runBlocking { dbQuery { SykmeldingStatusTable.deleteAll() } }

    @Test
    fun `getNext returns null when no pending items exist`() = runTest {
        val result = repo.getNext()
        assertNull(result)
    }

    @Test
    fun `getNext returns pending item and updates status to SENDING`() = runTest {
        val sykmeldingId = UUID.randomUUID()
        insertSykmeldingStatus(sykmeldingId, SykmeldingStatusStatus.PENDING)

        val result = repo.getNext()

        assertNotNull(result)
        assertEquals(sykmeldingId, result.sykmeldingId)
        assertEquals(SykmeldingStatusStatus.SENDING, result.status)
    }

    @Test
    fun `getNext picks oldest by send_timestamp first`() = runTest {
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
    fun `getNext ignores non-PENDING items`() = runTest {
        insertSykmeldingStatus(UUID.randomUUID(), SykmeldingStatusStatus.SENDING)
        insertSykmeldingStatus(UUID.randomUUID(), SykmeldingStatusStatus.SENT)
        insertSykmeldingStatus(UUID.randomUUID(), SykmeldingStatusStatus.FAILED)

        val result = repo.getNext()
        assertNull(result)
    }

    @Test
    fun `getNext called twice returns different items`() = runTest {
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
    fun `resetHangingJobs resets SENDING and FAILED to PENDING`() = runTest {
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
        dbQuery {
            SykmeldingStatusTable.selectAll().toList().forEach { row ->
                assertEquals(SykmeldingStatusStatus.PENDING.name, row[SykmeldingStatusTable.status])
            }
        }
    }

    @Test
    fun `resetHangingJobs does not reset recent items`() = runTest {
        val recent = UUID.randomUUID()
        insertSykmeldingStatus(recent, SykmeldingStatusStatus.SENDING)

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(0, count)
    }

    @Test
    fun `resetHangingJobs does not reset PENDING or SENT items`() = runTest {
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

    private suspend fun insertSykmeldingStatus(
        sykmeldingId: UUID,
        status: SykmeldingStatusStatus,
        eventTimestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
        sendTimestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    ) {
        dbQuery {
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
