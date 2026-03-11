package modules.sykmeldinger.sykmelder.service

import io.mockk.coEvery
import io.mockk.mockk
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.test.runTest
import modules.sykmeldinger.sykmelder.Sykmelder
import modules.sykmeldinger.sykmelder.SykmelderMedHpr
import modules.sykmeldinger.sykmelder.SykmelderService
import modules.sykmeldinger.sykmelder.clients.btsys.BtsysClient
import modules.sykmeldinger.sykmelder.clients.hpr.HprClient

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
                fornavn = "Howard",
                mellomnavn = null,
                etternavn = "Walowicz",
            )
        coEvery { btsysClient.isSuspendert(ident, oppslagsdato) } returns false

        val result = sykmelderService.byHpr(hprNummer, oppslagsdato)

        assertIs<Sykmelder.MedSuspensjon>(result)
        assertEquals(hprNummer, result.hpr)
        assertEquals(ident, result.ident)
        assertEquals(false, result.suspendert)
    }

    @Test
    fun `should return FinnesIkke when sykmelder is not found in HPR`() = runTest {
        val hprNummer = "99999"
        val oppslagsdato = LocalDate.of(2026, 3, 11)

        coEvery { hprClient.getSykmelderByHpr(hprNummer) } returns null

        val result = sykmelderService.byHpr(hprNummer, oppslagsdato)

        assertIs<Sykmelder.FinnesIkke>(result)
        assertEquals(hprNummer, result.hpr)
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
                fornavn = "Howard",
                mellomnavn = null,
                etternavn = "Walowicz",
            )
        coEvery { btsysClient.isSuspendert(ident, oppslagsdato) } returns true

        val result = sykmelderService.byHpr(hprNummer, oppslagsdato)

        assertIs<Sykmelder.MedSuspensjon>(result)
        assertEquals(hprNummer, result.hpr)
        assertEquals(ident, result.ident)
        assertEquals(true, result.suspendert)
    }

    //    @Test
    //    fun `should handle BtsysException when btsys client throws`() = runTest {
    //        val hprNummer = "12345"
    //        val ident = "12345678901"
    //        val oppslagsdato = LocalDate.of(2026, 3, 11)
    //
    //        coEvery { hprClient.getSykmelderByHpr(hprNummer) } returns SykmelderMedHpr(
    //            hprNummer = hprNummer,
    //            ident = ident,
    //            fornavn = "Howard",
    //            mellomnavn = null,
    //            etternavn = "Walowicz",
    //        )
    //        coEvery { btsysClient.isSuspendert(ident, oppslagsdato) } throws BtsysException("Btsys
    // responded with status 500 Internal Server Error")
    //
    //        val result = sykmelderService.byHpr(hprNummer, oppslagsdato)
    //
    //        //TODO this needs to be asserted depending on how we handle the exceptions in the
    // service. Fix this after fixing the service. CUrrently it will fail with 500 internal server
    // error as the exception is not handled in the service.
    //    }
}
