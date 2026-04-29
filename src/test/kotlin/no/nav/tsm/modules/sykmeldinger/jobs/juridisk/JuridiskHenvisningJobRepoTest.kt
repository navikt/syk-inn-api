package no.nav.tsm.modules.sykmeldinger.jobs.juridisk

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingStatusTable
import no.nav.tsm.regulus.regula.JuridiskHenvisning
import no.nav.tsm.regulus.regula.JuridiskHenvisningLovverk
import no.nav.tsm.regulus.regula.JuridiskUtfall
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import no.nav.tsm.utils.WithPostgresql
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll

class JuridiskHenvisningJobRepoTest : WithPostgresql() {

    companion object {
        init {
            runMigrations(true)
            connect()
        }
    }

    private val repo = JuridiskHenvisningJobRepo()

    @BeforeTest
    fun cleanup() {
        runBlocking { dbQuery { JuridiskVurderingStatusTable.deleteAll() } }
    }

    @Test
    fun `getNext returns null when no pending items exist`() = runTest {
        val result = repo.getNext()
        assertNull(result)
    }

    @Test
    fun `getNext returns pending item and updates status to SENDING`() = runTest {
        val sykmeldingId = UUID.randomUUID()
        insertRuleStatus(sykmeldingId, JuridiskVurderingStatus.PENDING)

        val result = repo.getNext()

        assertNotNull(result)
        assertEquals(sykmeldingId, result.sykmeldingId)
        assertEquals(JuridiskVurderingStatus.SENDING, result.status)
    }

    @Test
    fun `getNext picks oldest pending item first`() = runTest {
        val older = UUID.randomUUID()
        val newer = UUID.randomUUID()
        insertRuleStatus(
            older,
            JuridiskVurderingStatus.PENDING,
            OffsetDateTime.now(ZoneOffset.UTC).minusHours(2),
        )
        insertRuleStatus(
            newer,
            JuridiskVurderingStatus.PENDING,
            OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
        )

        val result = repo.getNext()

        assertNotNull(result)
        assertEquals(older, result.sykmeldingId)
    }

    @Test
    fun `getNext ignores non-PENDING items`() = runTest {
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.SENDING)
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.DONE)
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.FAILED)

        val result = repo.getNext()
        assertNull(result)
    }

    @Test
    fun `getNext called twice returns different items`() = runTest {
        val first = UUID.randomUUID()
        val second = UUID.randomUUID()
        insertRuleStatus(
            first,
            JuridiskVurderingStatus.PENDING,
            OffsetDateTime.now(ZoneOffset.UTC).minusHours(2),
        )
        insertRuleStatus(
            second,
            JuridiskVurderingStatus.PENDING,
            OffsetDateTime.now(ZoneOffset.UTC).minusHours(1),
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
        insertRuleStatus(sending, JuridiskVurderingStatus.SENDING, oldEventTimestamp)
        insertRuleStatus(failed, JuridiskVurderingStatus.FAILED, oldEventTimestamp)

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(2, count)
        dbQuery {
            JuridiskVurderingStatusTable.selectAll().toList().forEach { row ->
                assertEquals(
                    JuridiskVurderingStatus.PENDING.name,
                    row[JuridiskVurderingStatusTable.status],
                )
            }
        }
    }

    @Test
    fun `resetHangingJobs does not reset recent items`() = runTest {
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.SENDING)

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(0, count)
    }

    @Test
    fun `resetHangingJobs does not reset PENDING or SENT items`() = runTest {
        val oldEventTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2)
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.PENDING, oldEventTimestamp)
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.DONE, oldEventTimestamp)

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(0, count)
    }

    private suspend fun insertRuleStatus(
        sykmeldingId: UUID,
        status: JuridiskVurderingStatus,
        eventTimestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    ) {
        dbQuery {
            JuridiskVurderingStatusTable.insert {
                it[JuridiskVurderingStatusTable.sykmeldingId] = sykmeldingId
                it[JuridiskVurderingStatusTable.status] = status.name
                it[JuridiskVurderingStatusTable.eventTimestamp] = eventTimestamp
                it[JuridiskVurderingStatusTable.juridiskVurdering] = testJuridiskVurdering()
            }
        }
    }
}

private fun testJuridiskVurdering() =
    listOf(
        RegulaJuridiskVurdering(
            henvisning =
                JuridiskHenvisning(
                    lovverk = JuridiskHenvisningLovverk.FOLKETRYGDLOVEN,
                    paragraf = "8-4",
                    ledd = 1,
                    punktum = null,
                    bokstav = null,
                ),
            input = mapOf("key" to "value"),
            tidsstempel = ZonedDateTime.now(ZoneOffset.UTC),
            fodselsnummer = "12345678901",
            utfall = JuridiskUtfall.VILKAR_OPPFYLT,
        )
    )
