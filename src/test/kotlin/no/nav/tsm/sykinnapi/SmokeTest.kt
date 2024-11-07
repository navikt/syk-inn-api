package no.nav.tsm.sykinnapi

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import no.nav.tsm.sykinnapi.controllers.SykmeldingApiController
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@EnableJwtTokenValidation
@EnableMockOAuth2Server
@SpringBootTest
internal class SmokeTest {
    @Autowired private val controller: SykmeldingApiController? = null

    @Test
    @Throws(Exception::class)
    fun contextLoads() {
        assertThat(controller).isNotNull()
    }
}
