package no.nav.tsm.modules.sykmeldinger.sykmelder

import arrow.core.left
import arrow.core.right
import io.kotest.matchers.equals.shouldEqual
import io.kotest.matchers.types.shouldBeTypeOf
import io.mockk.coEvery
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import no.nav.tsm.ktor.core.SimpleNavn
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler.SykmelderMedHpr
import no.nav.tsm.modules.sykmeldinger.sykmelder.clients.behandler.TsmBehandlerClient

class SykmelderServiceTest {
    private val tsmBehandlerClient = mockk<TsmBehandlerClient>()
    private val sykmelderService = SykmelderService(tsmBehandlerClient = tsmBehandlerClient)

    @Test
    fun `should return sykmelder with suspensjon info by hpr`() = runTest {
        val hprNummer = "12345"
        val ident = "12345678901"

        coEvery { tsmBehandlerClient.getSykmelderByHpr(hprNummer) } returns
            SykmelderMedHpr(
                    hprNummer = hprNummer,
                    ident = ident,
                    navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                    godkjenninger = emptyList(),
                    suspendert = false,
                )
                .right()

        val result = sykmelderService.byHpr(hprNummer).getOrNull()

        result.shouldBeTypeOf<Sykmelder.MedSuspensjon>()
        result.hpr shouldEqual hprNummer
        result.ident shouldEqual ident
        result.suspendert shouldEqual false
    }

    @Test
    fun `should return FinnesIkke when sykmelder is not found in HPR`() = runTest {
        val hprNummer = "99999"

        coEvery { tsmBehandlerClient.getSykmelderByHpr(hprNummer) } returns
            TsmBehandlerClient.BehandlerErrors.NotFound.left()

        val result = sykmelderService.byHpr(hprNummer).getOrNull()

        result.shouldBeTypeOf<Sykmelder.FinnesIkke>()
    }

    @Test
    fun `should return MedSuspensjon with suspendert true when sykmelder is suspended`() = runTest {
        val hprNummer = "13378010"
        val ident = "12345678901"

        coEvery { tsmBehandlerClient.getSykmelderByHpr(hprNummer) } returns
            SykmelderMedHpr(
                    hprNummer = hprNummer,
                    ident = ident,
                    navn = SimpleNavn(fornavn = "Test", mellomnavn = null, etternavn = "Test"),
                    godkjenninger = emptyList(),
                    suspendert = true,
                )
                .right()

        val result = sykmelderService.byHpr(hprNummer).getOrNull()

        result.shouldBeTypeOf<Sykmelder.MedSuspensjon>()
        result.hpr shouldEqual hprNummer
        result.ident shouldEqual ident
        result.suspendert shouldEqual true
    }
}
