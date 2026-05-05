package no.nav.tsm.modules.sykmeldinger.jobs.sykmelding.consume

import arrow.core.right
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.time.LocalDate
import java.time.OffsetDateTime
import kotlin.collections.listOf
import kotlinx.coroutines.test.runTest
import no.nav.tsm.core.common.SimpleNavn
import no.nav.tsm.modules.sykmeldinger.pdl.PdlClient
import no.nav.tsm.modules.sykmeldinger.pdl.PdlIdent
import no.nav.tsm.modules.sykmeldinger.pdl.PdlIdentgruppe
import no.nav.tsm.modules.sykmeldinger.pdl.PdlNavn
import no.nav.tsm.modules.sykmeldinger.pdl.PdlPerson
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder
import no.nav.tsm.modules.sykmeldinger.sykmelder.SykmelderService
import no.nav.tsm.sykmelding.input.core.model.RuleType
import no.nav.tsm.sykmelding.input.core.model.Sykmelding
import no.nav.tsm.sykmelding.input.core.model.SykmeldingMeta
import no.nav.tsm.sykmelding.input.core.model.SykmeldingRecord
import no.nav.tsm.sykmelding.input.core.model.ValidationResult
import no.nav.tsm.sykmelding.input.core.model.metadata.HelsepersonellKategori
import no.nav.tsm.sykmelding.input.core.model.metadata.MessageMetadata
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonId
import no.nav.tsm.sykmelding.input.core.model.metadata.PersonIdType
import org.junit.Test

class SykmeldingConsumerResourcesServiceTest {

    private val pdlClient = mockk<PdlClient>()
    private val sykmelderService = mockk<SykmelderService>()
    private val service = SykmeldingConsumerResourcesService(pdlClient, sykmelderService)

    @Test
    fun `resources should cache properly when fetching by fnr`() {
        runTest {
            val testIdent = "foo-bar-ident"
            val testHpr = "baz-hpr"

            coEvery { pdlClient.getPerson(testIdent) } returns
                createTestPdlPerson(testIdent).right()
            coEvery { sykmelderService.byIdent(testIdent, LocalDate.now()) } returns
                createNonSuspendedTestSykmelder(testIdent, testHpr).right()

            val ids = listOf(PersonId(id = "foo-bar-ident", PersonIdType.FNR))
            val record = createDigitalRecord(ids = ids)
            val result = service.getResourcesForSykmelding(record)

            result.shouldNotBeNull()
            result.shouldBeTypeOf<RecordWithResources.Nasjonal>()
            result.ident shouldBe testIdent
            result.hpr shouldBe testHpr

            coVerify(exactly = 1) { pdlClient.getPerson(any()) }
            coVerify(exactly = 1) { sykmelderService.byIdent(any(), any()) }

            repeat(1000) { service.getResourcesForSykmelding(record) }

            coVerify(exactly = 1) { pdlClient.getPerson(any()) }
            coVerify(exactly = 1) { sykmelderService.byIdent(any(), any()) }
        }
    }

    @Test
    fun `resources should cache properly when fetching by hpr`() {
        runTest {
            val testIdent = "foo-bar-ident"
            val testHpr = "baz-hpr"

            coEvery { pdlClient.getPerson(testIdent) } returns
                createTestPdlPerson(testIdent).right()
            coEvery { sykmelderService.byHpr(testHpr, LocalDate.now()) } returns
                createNonSuspendedTestSykmelder(testIdent, testHpr).right()

            val ids = listOf(PersonId(id = testHpr, PersonIdType.HPR))
            val record = createDigitalRecord(ids = ids)
            val result = service.getResourcesForSykmelding(record)

            result.shouldNotBeNull()
            result.shouldBeTypeOf<RecordWithResources.Nasjonal>()
            result.ident shouldBe testIdent
            result.hpr shouldBe testHpr

            coVerify(exactly = 1) { pdlClient.getPerson(any()) }
            coVerify(exactly = 1) { sykmelderService.byHpr(any(), any()) }

            repeat(1000) { service.getResourcesForSykmelding(record) }

            coVerify(exactly = 1) { pdlClient.getPerson(any()) }
            coVerify(exactly = 1) { sykmelderService.byHpr(any(), any()) }
        }
    }
}

private fun createTestPdlPerson(ident: String) =
    PdlPerson(
        navn = PdlNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
        foedselsdato = LocalDate.now().minusYears(34),
        identer = listOf(PdlIdent(ident, PdlIdentgruppe.FOLKEREGISTERIDENT, false)),
    )

private fun createNonSuspendedTestSykmelder(ident: String, hpr: String) =
    Sykmelder.MedSuspensjon(
        suspendert = false,
        navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Testesson"),
        godkjenninger = listOf(),
        ident = ident,
        hpr = hpr,
    )

private fun createDigitalRecord(ids: List<PersonId>): SykmeldingRecord {
    return SykmeldingRecord.Digital(
        metadata = MessageMetadata.Digital(orgnummer = "123456789"),
        sykmelding =
            Sykmelding.Digital(
                id = "sykmelding-id",
                sykmelder =
                    no.nav.tsm.sykmelding.input.core.model.Sykmelder(
                        ids = ids,
                        helsepersonellKategori = HelsepersonellKategori.LEGE,
                    ),
                metadata =
                    SykmeldingMeta.Digital(
                        mottattDato = OffsetDateTime.now(),
                        genDate = OffsetDateTime.now(),
                        avsenderSystem = mockk(),
                    ),
                aktivitet = emptyList(),
                pasient = mockk(),
                behandler = mockk(),
                bistandNav = mockk(),
                arbeidsgiver = mockk(),
                tilbakedatering = mockk(),
                medisinskVurdering = mockk(),
                utdypendeSporsmal = emptyList(),
            ),
        validation =
            ValidationResult(
                status = RuleType.OK,
                timestamp = OffsetDateTime.now(),
                rules = emptyList(),
            ),
    )
}
