package no.nav.tsm.syk_inn_api.pdf

import com.openhtmltopdf.outputdevice.helper.BaseRendererBuilder.FontStyle
import java.io.File
import org.apache.pdfbox.io.IOUtils
import org.slf4j.LoggerFactory

private val logger = LoggerFactory.getLogger(PdfResources.javaClass)

object PdfResources {
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

    fun loadFont(name: String): File {
        val stream = this.javaClass.getResourceAsStream("/pdf/fonts/$name")
            ?: throw IllegalArgumentException("Font file $name not found")

        val tempFile = File.createTempFile("font-", name).apply { deleteOnExit() }
        stream.use { it.copyTo(tempFile.outputStream()) }
        return tempFile
    }

    val colorProfile: ByteArray =
        IOUtils.toByteArray(this.javaClass.getResourceAsStream("/pdf/sRGB.icc"))

    val logo = this.javaClass.getResourceAsStream("/pdf/logo.svg")
        ?: throw IllegalArgumentException("Logo resource not found")

}

data class FontMeta(
    val family: String,
    val weight: Int,
    val style: FontStyle,
    val subset: Boolean,
    val fileName: String,
)

