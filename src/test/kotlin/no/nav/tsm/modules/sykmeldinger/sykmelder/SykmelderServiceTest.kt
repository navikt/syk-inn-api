package no.nav.tsm.modules.sykmeldinger.sykmelder

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import no.nav.tsm.core.common.SimpleNavn
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.btsys.BtsysClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.HprClient
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.hpr.SykmelderMedHpr

class SykmelderServiceTest {
    private val hprClient = mockk<HprClient>()
    private val btsysClient = mockk<BtsysClient>()
    private val sykmelderService = SykmelderService(btsys = btsysClient, helsenettProxy = hprClient)

    @Test
    fun `should return sykmelder with suspensjon info by hpr`() = runTest {
        val hprNummer = "12345"
        val ident = "12345678901"
        val oppslagsdato = LocalDate.now()

        coEvery { hprClient.getSykmelderByHpr(hprNummer) } returns
            SykmelderMedHpr(
                    hprNummer = hprNummer,
                    ident = ident,
                    navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                    godkjenninger = emptyList(),
                )
                .right()
        coEvery { btsysClient.isSuspendert(ident, oppslagsdato) } returns false.right()

        val result = sykmelderService.byHpr(hprNummer, oppslagsdato).getOrNull()

        result.shouldBeTypeOf<Sykmelder.MedSuspensjon>()
        result.hpr shouldEqual hprNummer
        result.ident shouldEqual ident
        result.suspendert shouldEqual false
    }

    @Test
    fun `should return FinnesIkke when sykmelder is not found in HPR`() = runTest {
        val hprNummer = "99999"
        val oppslagsdato = LocalDate.of(2026, 3, 11)

        coEvery { hprClient.getSykmelderByHpr(hprNummer) } returns
            HprClient.HprErrors.NotFound.left()

        val result = sykmelderService.byHpr(hprNummer, oppslagsdato).getOrNull()

        result.shouldBeTypeOf<Sykmelder.FinnesIkke>()
    }

    @Test
    fun `should return MedSuspensjon with suspendert true when sykmelder is suspended`() = runTest {
        val hprNummer = "13378010"
        val ident = "12345678901"
        val oppslagsdato = LocalDate.of(2026, 3, 11)

        coEvery { hprClient.getSykmelderByHpr(hprNummer) } returns
            SykmelderMedHpr(
                    hprNummer = hprNummer,
                    ident = ident,
                    navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                    godkjenninger = emptyList(),
                )
                .right()
        coEvery { btsysClient.isSuspendert(ident, oppslagsdato) } returns true.right()

        val result = sykmelderService.byHpr(hprNummer, oppslagsdato).getOrNull()

        result.shouldBeTypeOf<Sykmelder.MedSuspensjon>()
        result.hpr shouldEqual hprNummer
        result.ident shouldEqual ident
        result.suspendert shouldEqual true
    }
}
