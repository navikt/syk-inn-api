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
import no.nav.tsm.core.db.runFlywayMigrations
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingTable
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskHenvisning
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskUtfall
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskVurdering
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskVurderingResult
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.JuridiskVurderingStatus
import no.nav.tsm.modules.sykmeldinger.rules.juridisk.Lovverk
import no.nav.tsm.utils.WithPostgresql
import org.jetbrains.exposed.v1.jdbc.Database
import org.jetbrains.exposed.v1.jdbc.deleteAll
import org.jetbrains.exposed.v1.jdbc.insert
import org.jetbrains.exposed.v1.jdbc.selectAll
import org.jetbrains.exposed.v1.jdbc.transactions.transaction

class JuridiskJobRepoTest : WithPostgresql() {

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

    private val repo = JuridiskJobRepo()

    @BeforeTest
    fun cleanup() {
        transaction { JuridiskVurderingTable.deleteAll() }
    }

    @Test
    fun `getNext returns null when no pending items exist`() {
        val result = repo.getNext()
        assertNull(result)
    }

    @Test
    fun `getNext returns pending item and updates status to SENDING`() {
        val sykmeldingId = UUID.randomUUID()
        insertRuleStatus(sykmeldingId, JuridiskVurderingStatus.PENDING)

        val result = repo.getNext()

        assertNotNull(result)
        assertEquals(sykmeldingId, result.sykmeldingId)
        assertEquals(JuridiskVurderingStatus.SENDING, result.status)
    }

    @Test
    fun `getNext picks oldest pending item first`() {
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
    fun `getNext ignores non-PENDING items`() {
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.SENDING)
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.SENT)
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.FAILED)

        val result = repo.getNext()
        assertNull(result)
    }

    @Test
    fun `getNext called twice returns different items`() {
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
    fun `resetHangingJobs resets SENDING and FAILED to PENDING`() {
        val sending = UUID.randomUUID()
        val failed = UUID.randomUUID()
        val oldEventTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2)
        insertRuleStatus(sending, JuridiskVurderingStatus.SENDING, oldEventTimestamp)
        insertRuleStatus(failed, JuridiskVurderingStatus.FAILED, oldEventTimestamp)

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(2, count)
        transaction {
            JuridiskVurderingTable.selectAll().forEach { row ->
                assertEquals(
                    JuridiskVurderingStatus.PENDING.name,
                    row[JuridiskVurderingTable.status],
                )
            }
        }
    }

    @Test
    fun `resetHangingJobs does not reset recent items`() {
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.SENDING)

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(0, count)
    }

    @Test
    fun `resetHangingJobs does not reset PENDING or SENT items`() {
        val oldEventTimestamp = OffsetDateTime.now(ZoneOffset.UTC).minusHours(2)
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.PENDING, oldEventTimestamp)
        insertRuleStatus(UUID.randomUUID(), JuridiskVurderingStatus.SENT, oldEventTimestamp)

        val count = repo.resetHangingJobs(OffsetDateTime.now(ZoneOffset.UTC).minusHours(1))

        assertEquals(0, count)
    }

    private fun insertRuleStatus(
        sykmeldingId: UUID,
        status: JuridiskVurderingStatus,
        eventTimestamp: OffsetDateTime = OffsetDateTime.now(ZoneOffset.UTC),
    ) {
        transaction {
            JuridiskVurderingTable.insert {
                it[JuridiskVurderingTable.sykmeldingId] = sykmeldingId
                it[JuridiskVurderingTable.status] = status.name
                it[JuridiskVurderingTable.eventTimestamp] = eventTimestamp
                it[JuridiskVurderingTable.juridiskVurdering] = testJuridiskVurdering()
            }
        }
    }
}

private fun testJuridiskVurdering() =
    JuridiskVurderingResult(
        juridiskeVurderinger =
            listOf(
                JuridiskVurdering(
                    juridiskHenvisning =
                        JuridiskHenvisning(
                            lovverk = Lovverk.FOLKETRYGDLOVEN,
                            paragraf = "8-4",
                            ledd = 1,
                            punktum = null,
                            bokstav = null,
                        ),
                    input = mapOf("key" to "value"),
                    tidsstempel = ZonedDateTime.now(ZoneOffset.UTC),
                    utfall = JuridiskUtfall.VILKAR_OPPFYLT,
                )
            )
    )
