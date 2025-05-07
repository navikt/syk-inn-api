package no.nav.tsm.syk_inn_api.service

import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import java.util.*
import kotlin.test.assertEquals
import no.nav.tsm.regulus.regula.RegulaOutcome
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.model.Godkjenning
import no.nav.tsm.syk_inn_api.model.Kode
import no.nav.tsm.syk_inn_api.model.Sykmelder
import no.nav.tsm.syk_inn_api.model.sykmelding.Aktivitet
import no.nav.tsm.syk_inn_api.model.sykmelding.DiagnoseSystem
import no.nav.tsm.syk_inn_api.model.sykmelding.Hoveddiagnose
import no.nav.tsm.syk_inn_api.model.sykmelding.Sykmelding
import no.nav.tsm.syk_inn_api.model.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.repository.SykmeldingRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SykmeldingServiceTest {
    private lateinit var sykmeldingService: SykmeldingService
    private lateinit var helsenettProxyService: HelsenettProxyService
    private lateinit var ruleService: RuleService
    private lateinit var sykmeldingRepository: SykmeldingRepository

    val behandlerHpr = "123456789"
    val sykmeldingId = UUID.randomUUID().toString()

    @BeforeEach
    fun setup() {
        helsenettProxyService = mockk()
        ruleService = mockk()
        sykmeldingRepository = mockk()
        sykmeldingService =
            SykmeldingService(
                sykmeldingRepository = sykmeldingRepository,
                ruleService = ruleService,
                helsenettProxyService = helsenettProxyService,,
            )
    }

    @Test
    fun `create sykmelding with valid data`() {
        every { helsenettProxyService.getSykmelderByHpr(behandlerHpr, any()) } returns
            Sykmelder(
                hprNummer = behandlerHpr,
                fnr = "12345678901",
                fornavn = "Ola",
                mellomnavn = null,
                etternavn = "Nordmann",
                godkjenninger =
                    listOf(
                        Godkjenning(
                            helsepersonellkategori =
                                Kode(
                                    aktiv = true,
                                    oid = 0,
                                    verdi = "LE",
                                ),
                            autorisasjon =
                                Kode(
                                    aktiv = true,
                                    oid = 7704,
                                    verdi = "1",
                                ),
                            tillegskompetanse = null,
                        ),
                    ),
            )

        every { ruleService.validateRules(any(), any(), any(), foedselsdato) } returns
            RegulaResult(
                status = RegulaStatus.OK,
                outcome =
                    RegulaOutcome(
                        status = RegulaStatus.OK,
                        rule = "the rule",
                        messageForUser = "message for user",
                        messageForSender = "message for sender",
                    ),
                results = emptyList(),
            )

        // TODO implement sykmeldingRepository.save after repository is real

        // TODO implement kafka sending after kafka is real

        val result =
            sykmeldingService.createSykmelding(
                payload =
                    SykmeldingPayload(
                        pasientFnr = "12345678901",
                        sykmelderHpr = "123456789",
                        sykmelding =
                            Sykmelding(
                                hoveddiagnose =
                                    Hoveddiagnose(
                                        system = DiagnoseSystem.ICD10,
                                        code = "S017",
                                    ),
                                aktivitet =
                                    Aktivitet.IkkeMulig(
                                        fom = "2020-01-01",
                                        tom = "2020-01-30",
                                    ),
                            ),
                        legekontorOrgnr = "987654321",
                    ),
            )

        assertEquals(201, result.statusCode.value())
    }

    @Test
    fun `failing to create sykmelding because of rule tree hit`() {
        every { helsenettProxyService.getSykmelderByHpr(behandlerHpr, any()) } returns
            Sykmelder(
                hprNummer = behandlerHpr,
                fnr = "12345678901",
                fornavn = "Ola",
                mellomnavn = null,
                etternavn = "Nordmann",
                godkjenninger =
                    listOf(
                        Godkjenning(
                            helsepersonellkategori =
                                Kode(
                                    aktiv = true,
                                    oid = 0,
                                    verdi = "LE",
                                ),
                            autorisasjon =
                                Kode(
                                    aktiv = true,
                                    oid = 7704,
                                    verdi = "1",
                                ),
                            tillegskompetanse = null,
                        ),
                    ),
            )

        every { ruleService.validateRules(any(), any(), any(), foedselsdato) } returns
            RegulaResult(
                status = RegulaStatus.INVALID,
                outcome =
                    RegulaOutcome(
                        status = RegulaStatus.INVALID,
                        rule = "the rule that failed",
                        messageForUser = "validation failed",
                        messageForSender = "message for sender",
                    ),
                results = emptyList(),
            )
        val result =
            sykmeldingService.createSykmelding(
                payload =
                    SykmeldingPayload(
                        pasientFnr = "12345678901",
                        sykmelderHpr = "123456789",
                        sykmelding =
                            Sykmelding(
                                hoveddiagnose =
                                    Hoveddiagnose(
                                        system = DiagnoseSystem.ICD10,
                                        code = "Z01",
                                    ),
                                aktivitet =
                                    Aktivitet.IkkeMulig(
                                        fom = "2020-01-01",
                                        tom = "2020-01-30",
                                    ),
                            ),
                        legekontorOrgnr = "987654321",
                    ),
            )

        // TODO implement sykmeldingRepository.save after repository is real
        // TODO implement kafka sending after kafka is real

        assertEquals(400, result.statusCode.value())
    }
}
