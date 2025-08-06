package no.nav.tsm.syk_inn_api.pdf

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.util.*

@WebMvcTest(PdfController::class)
class PdfControllerSimpleTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var pdfService: PdfService

    @Test
    fun `should fail GET pdf when missing HPR value`() {
        // given
        val sykmeldingId = UUID.randomUUID()

        // when / then
        mockMvc
            .perform(
                get("/api/sykmelding/$sykmeldingId/pdf")
                    .header("HPR", "")
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN),
            )
            .andExpect(status().isBadRequest)
            .andExpect(content().string("Missing HPR parameter"))
    }

    @Test
    fun `HPR header should not be case sensitive`() {
        // given
        val sykmeldingId = UUID.randomUUID()

        // when / then
        mockMvc
            .perform(
                get("/api/sykmelding/$sykmeldingId/pdf")
                    .header("HPR", "")
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN),
            )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `GET pdf should require HPR key`() {
        // given
        val sykmeldingId = UUID.randomUUID()

        // when / then
        mockMvc
            .perform(
                get("/api/sykmelding/$sykmeldingId/pdf")
                    .accept(MediaType.APPLICATION_JSON, MediaType.TEXT_PLAIN),
            )
            .andExpect(status().isBadRequest)
    }
}
