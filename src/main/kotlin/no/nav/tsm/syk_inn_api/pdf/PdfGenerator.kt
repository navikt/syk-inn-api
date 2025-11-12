package no.nav.tsm.syk_inn_api.pdf

import com.openhtmltopdf.extend.impl.FSDefaultCacheStore
import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder
import com.openhtmltopdf.pdfboxout.PDFontSupplier
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder
import com.openhtmltopdf.svgsupport.BatikSVGDrawer
import com.openhtmltopdf.util.XRLog
import io.opentelemetry.api.trace.Span
import io.opentelemetry.instrumentation.annotations.WithSpan
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import no.nav.tsm.syk_inn_api.utils.failSpan
import no.nav.tsm.syk_inn_api.utils.logger
import org.apache.fontbox.ttf.TTFParser
import org.apache.fontbox.ttf.TrueTypeFont
import org.apache.pdfbox.io.RandomAccessReadBuffer
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.font.PDType0Font
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.util.FileCopyUtils
import org.verapdf.gf.foundry.VeraGreenfieldFoundryProvider
import org.verapdf.pdfa.Foundries
import org.verapdf.pdfa.flavours.PDFAFlavour
import org.verapdf.pdfa.results.TestAssertion

@Service
class PdfGenerator() {
    private val logger = logger()

    companion object {
        private val FONT_CACHE: MutableMap<String, TrueTypeFont> = HashMap()
        private val REAL_FONT_CACHE = FSDefaultCacheStore()

        val colorProfile: ByteArray
            get() {
                val cpr = ClassPathResource("pdf/sRGB.icc")
                return FileCopyUtils.copyToByteArray(cpr.inputStream)
            }
    }

    init {
        XRLog.setLoggingEnabled(false)
    }

    @WithSpan
    fun generatePDFA(html: String): ByteArray {
        val span = Span.current()
        val outputStream = ByteArrayOutputStream()

        try {
            PdfRendererBuilder()
                .apply { fontSuppliers(this) }
                .useSVGDrawer(BatikSVGDrawer())
                .useColorProfile(colorProfile)
                .usePdfAConformance(PdfRendererBuilder.PdfAConformance.PDFA_2_A)
                .usePdfUaAccessibility(true)
                .withHtmlContent(html, null)
                .toStream(outputStream)
                .run()

            val byteArray = outputStream.toByteArray()
            span.setAttribute("PdfGenerator.bytes", byteArray.size.toLong())
            return byteArray
        } catch (e: Exception) {
            throw RuntimeException("Feil ved generering av pdf", e.failSpan(span))
        }
    }

    fun verifyPDFACompliance(
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

    private fun fontSuppliers(builder: PdfRendererBuilder) {
        val sourceSansProFamily = "Source Sans Pro"
        builder.useFont(
            fontSupplier("SourceSansPro-Regular.ttf"),
            sourceSansProFamily,
            400,
            BaseRendererBuilder.FontStyle.NORMAL,
            true,
        )
        builder.useFont(
            fontSupplier("SourceSansPro-Bold.ttf"),
            sourceSansProFamily,
            700,
            BaseRendererBuilder.FontStyle.OBLIQUE,
            true,
        )
        builder.useFont(
            fontSupplier("SourceSansPro-Italic.ttf"),
            sourceSansProFamily,
            400,
            BaseRendererBuilder.FontStyle.ITALIC,
            true,
        )
    }

    private fun fontSupplier(fontName: String): PDFontSupplier {
        if (FONT_CACHE.containsKey(fontName)) {
            val font = FONT_CACHE[fontName] ?: error("Kunne ikke finne font i cache")
            return pdfontSupplier(font)
        }

        val fontStream =
            requireNotNull(
                this.javaClass.classLoader.getResourceAsStream("pdf/fonts/$fontName"),
            ) {
                "Font file $fontName not found in resources"
            }

        val font =
            TTFParser().parse(RandomAccessReadBuffer(fontStream)).also { it.isEnableGsub = false }

        FONT_CACHE[fontName] = font

        return pdfontSupplier(font)
    }

    private fun pdfontSupplier(font: TrueTypeFont): PDFontSupplier =
        PDFontSupplier(
            PDType0Font.load(
                PDDocument(),
                font,
                true,
            ),
        )
}
