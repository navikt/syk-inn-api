package no.nav.tsm.modules.sykmeldinger

import arrow.core.left
import arrow.core.right
import io.kotest.assertions.throwables.shouldThrowMessage
import io.kotest.matchers.comparables.shouldBeLessThan
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import no.nav.tsm.ktor.core.SimpleNavn
import no.nav.tsm.modules.sykmeldinger.db.sykmelding.SykmeldingRepo
import no.nav.tsm.modules.sykmeldinger.pdl.PdlArrowed
import no.nav.tsm.modules.sykmeldinger.rules.RuleService
import no.nav.tsm.modules.sykmeldinger.sykmelder.Sykmelder
import no.nav.tsm.modules.sykmeldinger.sykmelder.SykmelderService
import no.nav.tsm.pdl.Person
import no.nav.tsm.regulus.regula.RegulaJuridiskVurdering
import no.nav.tsm.regulus.regula.RegulaResult
import org.junit.Test

class SykmeldingerServiceTest {

    private val sykmelderService = mockk<SykmelderService>()
    private val pdlClient = mockk<PdlArrowed>()
    private val sykmeldingRepo = mockk<SykmeldingRepo>()
    private val ruleService = mockk<RuleService>()

    private val sykmeldingService =
        SykmeldingerService(
            pdlClient = pdlClient,
            sykmelderService = sykmelderService,
            repo = sykmeldingRepo,
            ruleService = ruleService,
        )

    @Test
    fun `should short circuit when one resource fails early`() = runTest {
        coEvery { pdlClient.getPerson(any()) } coAnswers
            {
                delay(2500.milliseconds)

                PdlArrowed.PdlErrors.UnknownError.left()
            }

        coEvery { sykmelderService.byHpr(any(), any()) } coAnswers
            {
                delay(5000.milliseconds)

                Sykmelder.FinnesIkke.right()
            }

        coEvery { sykmeldingRepo.allByIdent(any()) } coAnswers
            {
                delay(7500.milliseconds)

                emptyList()
            }

        val timedValue = measureTimedValue { sykmeldingService.verify(mockk(relaxed = true)) }

        timedValue.duration shouldBeLessThan 3000.milliseconds
    }

    @Test
    fun `should handle exceptions`() = runTest {
        coEvery { pdlClient.getPerson(any()) } coAnswers
            {
                delay(100.milliseconds)

                throw IllegalStateException("Some scary exception deep down")
            }

        coEvery { sykmelderService.byHpr(any(), any()) } coAnswers
            {
                delay(5000.milliseconds)

                Sykmelder.FinnesIkke.right()
            }

        coEvery { sykmeldingRepo.allByIdent(any()) } coAnswers
            {
                delay(7500.milliseconds)

                emptyList()
            }

        shouldThrowMessage("Some scary exception deep down") {
            sykmeldingService.verify(mockk(relaxed = true))
        }
    }

    @Test
    fun `should run all resources in parallel`() = runTest {
        coEvery { pdlClient.getPerson(any()) } coAnswers
            {
                delay(3000.milliseconds)

                Person(
                        navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                        foedselsdato = null,
                        identer = listOf(),
                        doed = false,
                        doedsdato = null,
                        falskIdent = false,
                    )
                    .right()
            }

        coEvery { sykmelderService.byHpr(any(), any()) } coAnswers
            {
                delay(3500.milliseconds)

                Sykmelder.FinnesIkke.right()
            }

        coEvery { sykmeldingRepo.allByIdent(any()) } coAnswers
            {
                delay(3500.milliseconds)

                emptyList()
            }

        every { ruleService.verify(any(), any(), any(), any()) } returns okRuleResultPair.right()

        val timedValue = measureTimedValue { sykmeldingService.verify(mockk(relaxed = true)) }

        timedValue.duration shouldBeLessThan 4000.milliseconds
        timedValue.value.getOrNull().shouldBeTypeOf<RegulaResult.Ok>()
    }
}

val okRuleResultPair: Pair<RegulaResult, List<RegulaJuridiskVurdering>> =
    RegulaResult.Ok(emptyList()) to emptyList()
