package no.nav.tsm.syk_inn_api.service

import io.mockk.Runs
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.just
import io.mockk.mockk
import java.time.LocalDate
import java.util.*
import kotlin.test.assertEquals
import no.nav.tsm.regulus.regula.RegulaOutcome
import no.nav.tsm.regulus.regula.RegulaOutcomeReason
import no.nav.tsm.regulus.regula.RegulaOutcomeStatus
import no.nav.tsm.regulus.regula.RegulaResult
import no.nav.tsm.regulus.regula.RegulaStatus
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.Navn
import no.nav.tsm.syk_inn_api.model.SykmeldingResult
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.repository.IntegrationTest
import no.nav.tsm.syk_inn_api.sykmelder.Godkjenning
import no.nav.tsm.syk_inn_api.sykmelder.HelsenettProxyService
import no.nav.tsm.syk_inn_api.sykmelder.Kode
import no.nav.tsm.syk_inn_api.sykmelder.Sykmelder
import no.nav.tsm.syk_inn_api.sykmelding.Hoveddiagnose
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.OpprettSykmeldingPayload
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingPayload
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingService
import no.nav.tsm.syk_inn_api.sykmelding.kafka.SykmeldingKafkaService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingDb
import no.nav.tsm.syk_inn_api.sykmelding.persistence.SykmeldingPersistenceService
import no.nav.tsm.syk_inn_api.sykmelding.persistence.toPGobject
import no.nav.tsm.syk_inn_api.sykmelding.response.ExistingSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.response.ExistingSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.ExistingSykmeldingHoveddiagnose
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingResponse
import no.nav.tsm.syk_inn_api.sykmelding.rules.RuleService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class SykmeldingServiceTest : IntegrationTest() {
    private lateinit var sykmeldingService: SykmeldingService
    private lateinit var helsenettProxyService: HelsenettProxyService
    private lateinit var ruleService: RuleService
    private lateinit var sykmeldingKafkaService: SykmeldingKafkaService
    private lateinit var sykmeldingPersistenceService: SykmeldingPersistenceService
    private lateinit var personService: PersonService

    val behandlerHpr = "123456789"
    val sykmeldingId = UUID.randomUUID().toString()
    val foedselsdato = LocalDate.of(1990, 1, 1)
    val navn = Navn(fornavn = "Ola", mellomnavn = null, etternavn = "Nordmann")

    @BeforeEach
    fun setup() {
        helsenettProxyService = mockk()
        ruleService = mockk()
        sykmeldingPersistenceService = mockk()
        sykmeldingKafkaService = mockk()
        personService = mockk()
        sykmeldingService =
            SykmeldingService(
                sykmeldingPersistenceService = sykmeldingPersistenceService,
                ruleService = ruleService,
                helsenettProxyService = helsenettProxyService,
                sykmeldingKafkaService = sykmeldingKafkaService,
                personService = personService,
            )
    }

    @Test
    fun `create sykmelding with valid data`() {
        every { helsenettProxyService.getSykmelderByHpr(behandlerHpr, any()) } returns
            Sykmelder(
                hprNummer = behandlerHpr,
                fnr = "01019078901",
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

        every { personService.getPersonByIdent(any()) } returns
            Person(navn = navn, fodselsdato = foedselsdato, ident = "123")
        every { ruleService.validateRules(any(), any(), any(), foedselsdato) } returns
            RegulaResult.Ok(
                emptyList(),
            )

        val sykmeldingResponse =
            SykmeldingResponse(
                sykmeldingId = sykmeldingId,
                pasientFnr = "01019078901",
                sykmelderHpr = behandlerHpr,
                sykmelding =
                    ExistingSykmelding(
                        hoveddiagnose =
                            ExistingSykmeldingHoveddiagnose(
                                system = DiagnoseSystem.ICD10,
                                code = "Z01",
                                text = "Ukjent diagnose",
                            ),
                        aktivitet =
                            ExistingSykmeldingAktivitet.IkkeMulig(
                                fom = "2020-01-01",
                                tom = "2020-01-30",
                            ),
                    ),
                legekontorOrgnr = "987654321",
            )
        every { sykmeldingPersistenceService.mapDatabaseEntityToSykmeldingResponse(any()) } returns
            sykmeldingResponse

        every { sykmeldingPersistenceService.saveSykmeldingPayload(any(), any()) } returns
            sykmeldingPersistenceService.mapDatabaseEntityToSykmeldingResponse(
                SykmeldingDb(
                    id = UUID.randomUUID(),
                    sykmeldingId = sykmeldingId,
                    pasientFnr = "01019078901",
                    sykmelderHpr = behandlerHpr,
                    legekontorOrgnr = "987654321",
                    sykmelding = getTestSykmelding().toPGobject(),
                ),
            )

        every { sykmeldingKafkaService.send(any(), any(), any(), any(), any()) } just Runs

        val result =
            sykmeldingService.createSykmelding(
                payload =
                    SykmeldingPayload(
                        pasientFnr = "01019078901",
                        sykmelderHpr = "123456789",
                        sykmelding =
                            OpprettSykmeldingPayload(
                                hoveddiagnose =
                                    Hoveddiagnose(
                                        system = DiagnoseSystem.ICD10,
                                        code = "S017",
                                    ),
                                opprettSykmeldingAktivitet =
                                    OpprettSykmeldingAktivitet.IkkeMulig(
                                        fom = "2020-01-01",
                                        tom = "2020-01-30",
                                    ),
                            ),
                        legekontorOrgnr = "987654321",
                    ),
            )

        assert(result is SykmeldingResult.Success)
        require(result is SykmeldingResult.Success)
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

        every { personService.getPersonByIdent(any()) } returns
            Person(navn = navn, fodselsdato = foedselsdato, ident = "123")

        every { ruleService.validateRules(any(), any(), any(), foedselsdato) } returns
            RegulaResult.NotOk(
                status = RegulaStatus.INVALID,
                outcome =
                    RegulaOutcome(
                        status = RegulaOutcomeStatus.INVALID,
                        rule = "the rule that failed",
                        reason = RegulaOutcomeReason("validation failed", "message for sender"),
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
                            OpprettSykmeldingPayload(
                                hoveddiagnose =
                                    Hoveddiagnose(
                                        system = DiagnoseSystem.ICD10,
                                        code = "Z01",
                                    ),
                                opprettSykmeldingAktivitet =
                                    OpprettSykmeldingAktivitet.IkkeMulig(
                                        fom = "2020-01-01",
                                        tom = "2020-01-30",
                                    ),
                            ),
                        legekontorOrgnr = "987654321",
                    ),
            )

        // TODO implement sykmeldingRepository.save after repository is real
        // TODO implement kafka sending after kafka is real
        assert(result is SykmeldingResult.Failure)
        require(result is SykmeldingResult.Failure)
        assertEquals(400, result.errorCode.value())
    }

    private fun getTestSykmelding(): OpprettSykmeldingPayload {
        return OpprettSykmeldingPayload(
            hoveddiagnose =
                Hoveddiagnose(
                    system = DiagnoseSystem.ICD10,
                    code = "Z01",
                ),
            opprettSykmeldingAktivitet =
                OpprettSykmeldingAktivitet.IkkeMulig(
                    fom = "2020-01-01",
                    tom = "2020-01-30",
                ),
        )
    }
}
