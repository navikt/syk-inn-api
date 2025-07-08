package no.nav.tsm.syk_inn_api.pdf

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import java.awt.Desktop
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.Navn
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingService
import no.nav.tsm.syk_inn_api.sykmelding.response.ArbeidsrelatertArsak
import no.nav.tsm.syk_inn_api.sykmelding.response.MedisinskArsak
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentMeta
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentSykmelder
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentValues
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocumentYrkesskade
import no.nav.tsm.sykmelding.input.core.model.ArbeidsrelatertArsakType
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PdfServiceTest {

    private val personServiceMock: PersonService = mockk()
    private val sykmeldingServiceMock: SykmeldingService = mockk()

    private val pdfService: PdfService =
        PdfService(
            pdfGenerator = PdfGenerator(),
            sykmeldingService = sykmeldingServiceMock,
            personService = personServiceMock,
        )

    val testSykmeldingUuid = UUID.fromString("fc5504ae-2384-411c-9b38-cac5fa5b4179")

    @BeforeEach
    fun setup() {
        clearAllMocks()

        every { personServiceMock.getPersonByIdent("12345678901") } returns
            Result.success(
                Person(
                    navn = Navn("Ola", "Mellomnavn", "Nordmann"),
                    ident = "12345678901",
                    fodselsdato = LocalDate.parse("1989-06-07"),
                ),
            )
    }

    @Test
    fun `should create a simple PDF`() {
        val simpleSykmelding =
            SykmeldingDocument(
                sykmeldingId = "sykmeldingId",
                meta =
                    SykmeldingDocumentMeta(
                        mottatt = OffsetDateTime.now(),
                        pasientIdent = "12345678901",
                        sykmelder =
                            SykmeldingDocumentSykmelder(
                                hprNummer = "123456789",
                                fornavn = "Magnar",
                                mellomnavn = null,
                                etternavn = "Koman",
                            ),
                        legekontorOrgnr = "123456789",
                        legekontorTlf = "12345678",
                    ),
                values =
                    SykmeldingDocumentValues(
                        hoveddiagnose =
                            SykmeldingDocumentDiagnoseInfo(
                                system = DiagnoseSystem.ICPC2,
                                code = "X01",
                                text = "Hodepine",
                            ),
                        aktivitet =
                            listOf(
                                SykmeldingDocumentAktivitet.IkkeMulig(
                                    fom = "2023-01-01",
                                    tom = "2023-01-10",
                                    medisinskArsak = MedisinskArsak(isMedisinskArsak = true),
                                    arbeidsrelatertArsak = ArbeidsrelatertArsak(isArbeidsrelatertArsak = true, arbeidsrelaterteArsaker = listOf(
                                        ArbeidsrelatertArsakType.MANGLENDE_TILRETTELEGGING,
                                        ArbeidsrelatertArsakType.ANNET), andreArbeidsrelaterteArsaker = "Test")
                                ),
                                SykmeldingDocumentAktivitet.Gradert(
                                    fom = "2023-01-11",
                                    tom = "2023-01-15",
                                    grad = 60,
                                    reisetilskudd = false
                                ),
                            ),
                        bidiagnoser = emptyList(),
                        svangerskapsrelatert = true,
                        yrkesskade =
                            SykmeldingDocumentYrkesskade(
                                yrkesskade = true,
                                skadedato = LocalDate.parse("1989-06-07"),
                            ),
                        pasientenSkalSkjermes = false,
                        meldinger =
                            SykmeldingDocumentMeldinger(
                                tilNav = null,
                                tilArbeidsgiver =
                                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                            ),
                        arbeidsgiver = null,
                        tilbakedatering = null,
                    ),
                utfall =
                    SykmeldingDocumentRuleResult(
                        result = "OK",
                        melding = null,
                    ),
            )

        every { sykmeldingServiceMock.getSykmeldingById(testSykmeldingUuid, "123456789") } returns
            simpleSykmelding

        val pdf = pdfService.createSykmeldingPdf(testSykmeldingUuid, "123456789").getOrThrow()

        assertNotNull(pdf)

        // Uncomment this to open the PDF in the default viewer, for developing PDF
        openPdf(pdf, temp = false)
    }
}

fun openPdf(bytes: ByteArray, temp: Boolean = true) {
    if (temp) {
        val tmpFile = kotlin.io.path.createTempFile(suffix = ".pdf").toFile()
        tmpFile.writeBytes(bytes)
        Desktop.getDesktop().open(tmpFile)
    } else {
        // Write to example.pdf in the current directory
        val outputStream: OutputStream = FileOutputStream("example.pdf")
        outputStream.write(bytes)
        outputStream.close()
        Desktop.getDesktop().open(File("example.pdf"))
    }
}
