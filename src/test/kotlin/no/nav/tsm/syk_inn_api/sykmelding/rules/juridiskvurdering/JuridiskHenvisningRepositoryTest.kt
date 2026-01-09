package no.nav.tsm.syk_inn_api.sykmelding.rules.juridiskvurdering

import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import no.nav.syfo.rules.juridiskvurdering.JuridiskHenvisning
import no.nav.syfo.rules.juridiskvurdering.JuridiskUtfall
import no.nav.syfo.rules.juridiskvurdering.JuridiskVurdering
import no.nav.syfo.rules.juridiskvurdering.Lovverk
import no.nav.tsm.syk_inn_api.sykmelding.rules.JuridiskHenvisningRepository
import no.nav.tsm.syk_inn_api.sykmelding.rules.JuridiskVurderingResult
import no.nav.tsm.syk_inn_api.test.FullIntegrationTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest
import org.springframework.context.annotation.Import

@JdbcTest
@Import(JuridiskHenvisningRepository::class)
class JuridiskHenvisningRepositoryTest : FullIntegrationTest() {

    @Autowired lateinit var juridiskHenvisningRepository: JuridiskHenvisningRepository

    @Test
    fun testHRepoOperations() {
        val noNext = juridiskHenvisningRepository.getNextToSend()

        assertEquals(null, noNext)
        assertEquals(
            0,
            juridiskHenvisningRepository.resetHangingJobs(OffsetDateTime.now().plusSeconds(1))
        )

        val juridiskVurderingResultat =
            JuridiskVurderingResult(
                listOf(
                    JuridiskVurdering(
                        id = "1",
                        eventName = "eventName",
                        version = "version",
                        kilde = "kilde",
                        versjonAvKode = "kode",
                        fodselsnummer = "fnr",
                        juridiskHenvisning =
                            JuridiskHenvisning(Lovverk.FOLKETRYGDLOVEN, "1", 1, 1, "a"),
                        sporing = mapOf("sykmeldingId" to "1"),
                        input = mapOf("sykmeldingId" to "1"),
                        tidsstempel = ZonedDateTime.now(ZoneOffset.UTC),
                        utfall = JuridiskUtfall.VILKAR_OPPFYLT
                    )
                )
            )

        val sykmeldingId = UUID.randomUUID()
        val saved =
            juridiskHenvisningRepository.insert(
                sykmeldingId,
                OffsetDateTime.now(),
                juridiskVurderingResultat
            )
        assertEquals(1, saved)
        assertEquals(
            0,
            juridiskHenvisningRepository.resetHangingJobs(OffsetDateTime.now().plusSeconds(1))
        )

        val next = juridiskHenvisningRepository.getNextToSend()

        requireNotNull(next)

        assertEquals(sykmeldingId, next.sykmeldingId)
        assertEquals(null, juridiskHenvisningRepository.getNextToSend())

        juridiskHenvisningRepository.markAsFailed(next)

        assertEquals(null, juridiskHenvisningRepository.getNextToSend())
        assertEquals(
            1,
            juridiskHenvisningRepository.resetHangingJobs(OffsetDateTime.now().plusSeconds(1))
        )

        val retry = juridiskHenvisningRepository.getNextToSend()
        assertEquals(sykmeldingId, retry?.sykmeldingId)

        juridiskHenvisningRepository.markAsSent(retry!!)
        assertEquals(null, juridiskHenvisningRepository.getNextToSend())
        assertEquals(
            0,
            juridiskHenvisningRepository.resetHangingJobs(OffsetDateTime.now().plusSeconds(1))
        )
    }
}
