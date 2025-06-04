package no.nav.tsm.syk_inn_api.pdf

import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
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
import no.nav.tsm.syk_inn_api.sykmelding.response.ExistingSykmeldingHoveddiagnose
import no.nav.tsm.syk_inn_api.sykmelding.response.SykmeldingResponse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class PdfServiceTest {

    private val personServiceMock: PersonService = mockk()
    private val sykmeldingServiceMock: SykmeldingService = mockk()

    private val pdfService: PdfService =
        PdfService(
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
            SykmeldingResponse(
                sykmeldingId = "sykmeldingId",
                pasientFnr = "12345678901",
                sykmelderHpr = "123456789",
                legekontorOrgnr = "123456789",
                sykmelding =
                    ExistingSykmelding(
                        hoveddiagnose =
                            ExistingSykmeldingHoveddiagnose(
                                system = DiagnoseSystem.ICPC2,
                                code = "X01",
                                text = "Hodepine",
                            ),
                        aktivitet =
                            ExistingSykmeldingAktivitet.IkkeMulig(
                                fom = "2023-01-01",
                                tom = "2023-01-10",
                            ),
                    ),
            )

        every { sykmeldingServiceMock.getSykmeldingById(testSykmeldingUuid, "123456789") } returns
            simpleSykmelding

        val pdf = pdfService.createSykmeldingPdf(testSykmeldingUuid, "123456789").getOrThrow()

        // Uncomment this to open the PDF in the default viewer, for developing PDF
        // openPdf(pdf, temp = false)

        assertNotNull(pdf)
    }
}

fun openPdf(bytes: ByteArray, temp: Boolean = true) {
    if (temp) {
        val tmpFile = kotlin.io.path.createTempFile(suffix = ".pdf").toFile()
        tmpFile.writeBytes(bytes)
        java.awt.Desktop.getDesktop().open(tmpFile)
    } else {
        // Write to example.pdf in the current directory
        val outputStream: java.io.OutputStream = java.io.FileOutputStream("example.pdf")
        outputStream.write(bytes)
        outputStream.close()
        java.awt.Desktop.getDesktop().open(java.io.File("example.pdf"))
    }
}
