package no.nav.tsm.sykinnapi

import no.nav.security.token.support.spring.api.EnableJwtTokenValidation
import no.nav.security.token.support.spring.test.EnableMockOAuth2Server
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest

@EnableJwtTokenValidation
@EnableMockOAuth2Server
@SpringBootTest
class SykInnApiApplicationTests {

    @Test fun contextLoads() {}
}
