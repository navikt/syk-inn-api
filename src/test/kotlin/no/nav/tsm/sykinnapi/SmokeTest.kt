package no.nav.tsm.sykinnapi

import no.nav.tsm.sykinnapi.controllers.SykmeldingApiController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
internal class SmokeTest {
    @Autowired private val controller: SykmeldingApiController? = null

    @Test
    @Throws(Exception::class)
    fun contextLoads() {
        assertThat(controller).isNotNull()
    }
}
