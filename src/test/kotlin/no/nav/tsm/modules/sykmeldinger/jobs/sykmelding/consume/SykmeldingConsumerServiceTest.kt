package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import no.nav.tsm.core.SykmeldingConfig
import no.nav.tsm.core.common.SykInnDiagnoseSystem
import no.nav.tsm.core.db.dbQuery
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingTable
import no.nav.tsm.modules.sykmeldinger.domain.*
import no.nav.tsm.utils.WithPostgresql
import org.jetbrains.exposed.v1.core.eq
import org.jetbrains.exposed.v1.r2dbc.deleteAll
import org.jetbrains.exposed.v1.r2dbc.selectAll

class SykmeldingConsumerServiceTest : WithPostgresql() {

    companion object {
        init {
            runMigrations(true)
            connect()
        }
    }

    private val consumer: SykmeldingConsumer = mockk()
    private val repo = SykmeldingConsumerRepo()
    private val service =
        SykmeldingConsumerService(
            environment = mockk(),
            sykmeldingConsumerRepo = repo,
            consumer = consumer,
            sykmeldingConfig = SykmeldingConfig(retention = 365.days),
        )

    @BeforeTest
    fun setup() = runTest {
        dbQuery { SykmeldingTable.deleteAll() }
        every { consumer.subscribe() } just Runs
        every { consumer.unsubscribe() } just Runs
    }

    @Test
    fun `inserts sykmelding when within retention period`() = runTest {
        val sykmelding = testSykmelding(tom = LocalDate.now())

        launch {
                every { consumer.poll() } returns
                    listOf(sykmelding.sykmeldingId.toString() to sykmelding) andThenAnswer
                    {
                        cancel("stopping coroutine by flipping the isActive property")
                        emptyList()
                    }

                service.consume()
            }
            .join()

        assertTrue(existsInDb(sykmelding.sykmeldingId))
    }

    @Test
    fun `skips sykmelding when over retention period`() = runTest {
        val sykmelding = testSykmelding(tom = LocalDate.now().minusDays(366))
        launch {
                every { consumer.poll() } returns
                    listOf(sykmelding.sykmeldingId.toString() to sykmelding) andThenAnswer
                    {
                        cancel("stopping coroutine by flipping the isActive property")
                        emptyList()
                    }
                service.consume()
            }
            .join()

        assertFalse(existsInDb(sykmelding.sykmeldingId))
    }

    @Test
    fun `deletes sykmelding when tombstone is received`() = runTest {
        val existing = testSykmelding(tom = LocalDate.now())
        repo.insert(existing)
        assertTrue(existsInDb(existing.sykmeldingId))

        launch {
                every { consumer.poll() } returns
                    listOf(existing.sykmeldingId.toString() to null) andThenAnswer
                    {
                        cancel()
                        emptyList()
                    }
                service.consume()
            }
            .join()
        assertFalse(existsInDb(existing.sykmeldingId))
    }

    private suspend fun existsInDb(id: UUID): Boolean = dbQuery {
        SykmeldingTable.selectAll()
            .where { SykmeldingTable.id eq id }
            .map { it[SykmeldingTable.id] }
            .firstOrNull() != null
    }

    private fun testSykmelding(
        id: UUID = UUID.randomUUID(),
        tom: LocalDate = LocalDate.now(),
    ): VerifiedSykInnSykmelding =
        VerifiedSykInnSykmelding(
            sykmeldingId = id,
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
            result = SykInnSykmeldingRuleResult.OK,
            type = SykInnSykmeldingType.DIGITAL,
        )
}
