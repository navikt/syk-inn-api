package no.nav.tsm.syk_inn_api.pdf

import com.openhtmltopdf.pdfboxout.PDFontSupplier
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import org.apache.fontbox.ttf.TTFParser
import org.apache.pdfbox.io.RandomAccessReadBufferedFile
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.slf4j.LoggerFactory
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.flavours.PDFAFlavour
import org.verapdf.pdfa.results.TestAssertion

private val logger = LoggerFactory.getLogger("no.nav.tsm.syk_inn_api.pdf.CreatePdf.kt")

fun createPDFA(html: String): ByteArray {
    val pdf =
        ByteArrayOutputStream()
            .apply {
                PdfRendererBuilder()
                    .apply {
                        for (font in PdfResources.pdfFonts) {
                            val ttf =
                                TTFParser()
                                    .parse(
                                        RandomAccessReadBufferedFile(
                                            PdfResources.loadFont(font.fileName),
                                        ),
                                    )
                                    .also { it.isEnableGsub = false }
                            useFont(
                                PDFontSupplier(PDType0Font.load(PDDocument(), ttf, font.subset)),
                                font.family,
                                font.weight,
                                font.style,
                                font.subset,
                            )
                        }
                    }
                    .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_A)
                    .usePdfUaAccessibility(true)
                    .useColorProfile(PdfResources.colorProfile)
                    .useSVGDrawer(BatikSVGDrawer())
                    .withHtmlContent(html, null)
                    .toStream(this)
                    .run()
            }
            .toByteArray()

    require(verifyPDFACompliance(pdf)) { "Non-compliant PDF/A :(" }

    return pdf
}

private fun verifyPDFACompliance(
    input: ByteArray,
    flavour: PDFAFlavour = PDFAFlavour.PDFA_2_A,
): Boolean {
    VeraGreenfieldFoundryProvider.initialise()

    val pdf = ByteArrayInputStream(input)
    val validator = Foundries.defaultInstance().createValidator(flavour, false)
    val result = Foundries.defaultInstance().createParser(pdf).use { validator.validate(it) }
    val failures = result.testAssertions.filter { it.status != TestAssertion.Status.PASSED }
    failures.forEach { test ->
        logger.warn(test.message)
        logger.warn("Location ${test.location.context} ${test.location.level}")
        logger.warn("Status ${test.status}")
        logger.warn("Test number ${test.ruleId.testNumber}")
    }
    return failures.isEmpty()
}
