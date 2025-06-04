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
import java.util.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import no.nav.tsm.syk_inn_api.common.DiagnoseSystem
import no.nav.tsm.syk_inn_api.common.Navn
import no.nav.tsm.syk_inn_api.person.Person
import no.nav.tsm.syk_inn_api.person.PersonService
import no.nav.tsm.syk_inn_api.sykmelding.SykmeldingService
import no.nav.tsm.syk_inn_api.sykmelding.response.ExistingSykmelding
import no.nav.tsm.syk_inn_api.sykmelding.response.ExistingSykmeldingAktivitet
import no.nav.tsm.syk_inn_api.sykmelding.response.ExistingSykmeldingDiagnoseInfo
import no.nav.tsm.syk_inn_api.sykmelding.response.ExistingSykmeldingMeldinger
import no.nav.tsm.syk_inn_api.sykmelding.response.ExistingSykmeldingRuleResult
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingDocument
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
                pasientFnr = "12345678901",
                sykmelderHpr = "123456789",
                legekontorOrgnr = "123456789",
                sykmelding =
                    ExistingSykmelding(
                        hoveddiagnose =
                            ExistingSykmeldingDiagnoseInfo(
                                system = DiagnoseSystem.ICPC2,
                                code = "X01",
                                text = "Hodepine",
                            ),
                        aktivitet =
                            listOf(
                                ExistingSykmeldingAktivitet.IkkeMulig(
                                    fom = "2023-01-01",
                                    tom = "2023-01-10",
                                )
                            ),
                        bidiagnoser = emptyList(),
                        svangerskapsrelatert = false,
                        pasientenSkalSkjermes = false,
                        meldinger =
                            ExistingSykmeldingMeldinger(tilNav = null, tilArbeidsgiver = null),
                        yrkesskade = null,
                        arbeidsgiver = null,
                        tilbakedatering = null,
                        regelResultat =
                            ExistingSykmeldingRuleResult(
                                result = "OK",
                                meldingTilSender = null,
                            ),
                    )
            )

        every { sykmeldingServiceMock.getSykmeldingById(testSykmeldingUuid, "123456789") } returns
            simpleSykmelding

        val pdf = pdfService.createSykmeldingPdf(testSykmeldingUuid, "123456789").getOrThrow()

        assertNotNull(pdf)

        // Uncomment this to open the PDF in the default viewer, for developing PDF
        // openPdf(pdf, temp = false)
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
