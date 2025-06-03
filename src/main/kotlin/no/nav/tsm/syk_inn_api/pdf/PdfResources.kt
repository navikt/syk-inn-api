package no.nav.tsm.syk_inn_api.pdf

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle
import java.io.File
import org.apache.pdfbox.io.IOUtils
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PdfResources.javaClass)

object PdfResources {
    private val fontsFolder = File(this.javaClass.getResource("/pdf/fonts").toURI())

    val pdfFonts =
        listOf(
            FontMeta(
                family = "Source Sans Pro",
                weight = 400,
                style = FontStyle.NORMAL,
                subset = false,
                fileName = "SourceSansPro-Regular.ttf",
            ),
            FontMeta(
                family = "Source Sans Pro",
                weight = 700,
                style = FontStyle.NORMAL,
                subset = false,
                fileName = "SourceSansPro-Bold.ttf",
            ),
            FontMeta(
                family = "Source Sans Pro",
                weight = 400,
                style = FontStyle.ITALIC,
                subset = false,
                fileName = "SourceSansPro-Italic.ttf",
            ),
        )

    fun loadFont(name: String) =
        File(fontsFolder, name).also {
            if (!it.exists()) {
                throw IllegalArgumentException("Font file $name not found in $fontsFolder")
            } else {
                logger.info("Loading font: ${it.path}")
            }
        }


    val colorProfile: ByteArray =
        IOUtils.toByteArray(object {}::class.java.getResourceAsStream("/pdf/sRGB.icc"))

    val logo =
        object {}.javaClass.getResourceAsStream("/pdf/logo.svg")
            ?: throw IllegalArgumentException("Logo resource not found")

}

data class FontMeta(
    val family: String,
    val weight: Int,
    val style: FontStyle,
    val subset: Boolean,
    val fileName: String,
)

