package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.delete

import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.status.JuridiskVurderingStatusTable
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusStatus
import no.nav.tsm.modules.sykmeldinger.db.status.SykmeldingStatusTable
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable
import no.nav.tsm.modules.sykmeldinger.domain.SykInnAktivitet
import no.nav.tsm.modules.sykmeldinger.domain.SykInnBehandler
import no.nav.tsm.modules.sykmeldinger.domain.SykInnDiagnoseInfo
import no.nav.tsm.modules.sykmeldinger.domain.SykInnPasient
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingMeta
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingRuleResult
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingType
import no.nav.tsm.modules.sykmeldinger.domain.SykInnSykmeldingValues
import no.nav.tsm.modules.sykmeldinger.domain.VerifiedSykInnSykmelding
import no.nav.tsm.modules.sykmeldinger.jobs.juridisk.JuridiskVurderingStatus
import no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume.SykmeldingConsumerRepo
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import no.nav.tsm.utils.WithPostgresql
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.insert
import org.jetbrains.exposed.v1.r2dbc.selectAll

class SykmeldingDeleteRepoTest : WithPostgresql() {
    companion object {
        init {
            runMigrations(true)
            connect()
        }
    }

    private val repo = SykmeldingDeleteRepo(config)
    private val consumerRepo = SykmeldingConsumerRepo()

    @BeforeTest
    fun cleanup(): Unit = runBlocking {
        dbQuery {
            JuridiskVurderingStatusTable.deleteAll()
            SykmeldingStatusTable.deleteAll()
            SykmeldingTable.deleteAll()
        }
    }

    @Test
    fun `null+null = delete`() = runTest {
        val sykmeldingId = UUID.randomUUID()

        insertStaleSykmelding(sykmeldingId)

        val deletedCount = repo.deleteStaleSykmeldinger()

        assertEquals(1, deletedCount)
        assertFalse(existsInDb(sykmeldingId))
    }

    @Test
    fun `SENT+null = delete`() = runTest {
        val sykmeldingId = UUID.randomUUID()

        insertStaleSykmelding(sykmeldingId)
        insertSykmeldingStatus(sykmeldingId, SykmeldingStatusStatus.SENT)

        val deletedCount = repo.deleteStaleSykmeldinger()

        assertEquals(1, deletedCount)
        assertFalse(existsInDb(sykmeldingId))
    }

    @Test
    fun `SENT+DONE = delete`() = runTest {
        val sykmeldingId = UUID.randomUUID()

        insertStaleSykmelding(sykmeldingId)
        insertSykmeldingStatus(sykmeldingId, SykmeldingStatusStatus.SENT)
        insertJuridiskStatus(sykmeldingId, JuridiskVurderingStatus.DONE)

        val deletedCount = repo.deleteStaleSykmeldinger()

        assertEquals(1, deletedCount)
        assertFalse(existsInDb(sykmeldingId))
    }

    @Test
    fun `null+DONE = delete`() = runTest {
        val sykmeldingId = UUID.randomUUID()

        insertStaleSykmelding(sykmeldingId)
        insertJuridiskStatus(sykmeldingId, JuridiskVurderingStatus.DONE)

        val deletedCount = repo.deleteStaleSykmeldinger()

        assertEquals(1, deletedCount)
        assertFalse(existsInDb(sykmeldingId))
    }

    @Test
    fun `FAILED+DONE = NOT delete`() = runTest {
        val sykmeldingId = UUID.randomUUID()

        insertStaleSykmelding(sykmeldingId)
        insertSykmeldingStatus(sykmeldingId, SykmeldingStatusStatus.FAILED)
        insertJuridiskStatus(sykmeldingId, JuridiskVurderingStatus.DONE)

        val deletedCount = repo.deleteStaleSykmeldinger()

        assertEquals(0, deletedCount)
        assertTrue(existsInDb(sykmeldingId))
    }

    @Test
    fun `PENDING+DONE = NOT delete`() = runTest {
        val sykmeldingId = UUID.randomUUID()

        insertStaleSykmelding(sykmeldingId)
        insertSykmeldingStatus(sykmeldingId, SykmeldingStatusStatus.PENDING)
        insertJuridiskStatus(sykmeldingId, JuridiskVurderingStatus.DONE)

        val deletedCount = repo.deleteStaleSykmeldinger()

        assertEquals(0, deletedCount)
        assertTrue(existsInDb(sykmeldingId))
    }

    @Test
    fun `SENT+PENDING = NOT delete`() = runTest {
        val sykmeldingId = UUID.randomUUID()

        insertStaleSykmelding(sykmeldingId)
        insertSykmeldingStatus(sykmeldingId, SykmeldingStatusStatus.SENT)
        insertJuridiskStatus(sykmeldingId, JuridiskVurderingStatus.PENDING)

        val deletedCount = repo.deleteStaleSykmeldinger()

        assertEquals(0, deletedCount)
        assertTrue(existsInDb(sykmeldingId))
    }

    @Test
    fun `SENT+FAILED = NOT delete`() = runTest {
        val sykmeldingId = UUID.randomUUID()

        insertStaleSykmelding(sykmeldingId)
        insertSykmeldingStatus(sykmeldingId, SykmeldingStatusStatus.SENT)
        insertJuridiskStatus(sykmeldingId, JuridiskVurderingStatus.FAILED)

        val deletedCount = repo.deleteStaleSykmeldinger()

        assertEquals(0, deletedCount)
        assertTrue(existsInDb(sykmeldingId))
    }

    private suspend fun insertStaleSykmelding(id: UUID) {
        consumerRepo.insert(testSykmelding(id = id, tom = LocalDate.now().minusDays(15)))
    }

    private suspend fun insertSykmeldingStatus(sykmeldingId: UUID, status: SykmeldingStatusStatus) {
        dbQuery {
            SykmeldingStatusTable.insert {
                it[SykmeldingStatusTable.sykmeldingId] = sykmeldingId
                it[SykmeldingStatusTable.status] = status.name
                it[SykmeldingStatusTable.eventTimestamp] = OffsetDateTime.now()
                it[SykmeldingStatusTable.mottattTimestamp] = OffsetDateTime.now()
                it[SykmeldingStatusTable.sendTimestamp] = OffsetDateTime.now()
                it[SykmeldingStatusTable.sourceSystem] = "test"
            }
        }
    }

    private suspend fun insertJuridiskStatus(sykmeldingId: UUID, status: JuridiskVurderingStatus) {
        dbQuery {
            JuridiskVurderingStatusTable.insert {
                it[JuridiskVurderingStatusTable.sykmeldingId] = sykmeldingId
                it[JuridiskVurderingStatusTable.status] = status.name
                it[JuridiskVurderingStatusTable.eventTimestamp] = OffsetDateTime.now()
                it[JuridiskVurderingStatusTable.juridiskVurdering] =
                    emptyList<RegulaJuridiskVurdering>()
            }
        }
    }

    private suspend fun existsInDb(id: UUID): Boolean = dbQuery {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.id eq id }
            .map { it[SykmeldingTable.id] }
            .toList()
            .isNotEmpty()
    }

    private fun testSykmelding(
        id: UUID = UUID.randomUUID(),
        tom: LocalDate,
    ): VerifiedSykInnSykmelding =
        VerifiedSykInnSykmelding(
            sykmeldingId = id,
            result = SykInnSykmeldingRuleResult.OK,
            type = SykInnSykmeldingType.DIGITAL,
            values =
                SykInnSykmeldingValues(
                    pasientenSkalSkjermes = false,
                    hoveddiagnose = SykInnDiagnoseInfo(SykInnDiagnoseSystem.ICPC2, "L73"),
                    bidiagnoser = emptyList(),
                    aktivitet =
                        listOf(
                            SykInnAktivitet.IkkeMulig(
                                fom = tom.minusDays(7),
                                tom = tom,
                                arbeidsrelatertArsak = null,
                            )
                        ),
                    svangerskapsrelatert = false,
                    meldinger = null,
                    yrkesskade = null,
                    arbeidsgiver = null,
                    tilbakedatering = null,
                    utdypendeSporsmal = null,
                    annenFravarsgrunn = null,
                ),
            meta =
                SykInnSykmeldingMeta.Digital(
                    source = "test",
                    mottatt = OffsetDateTime.now(),
                    pasient =
                        SykInnPasient(
                            fornavn = "Pasient",
                            mellomnavn = null,
                            etternavn = "Pasientsen",
                            ident = "12345678901",
                        ),
                    behandler =
                        SykInnBehandler(
                            fornavn = "Lege",
                            mellomnavn = null,
                            etternavn = "Legesen",
                            hpr = "9144889",
                            helsepersonellkategori = listOf("LE"),
                            fnr = "12345678901",
                        ),
                    legekontorOrgnr = "123456789",
                    legekontorTlf = "12345678",
                ),
        )
}
