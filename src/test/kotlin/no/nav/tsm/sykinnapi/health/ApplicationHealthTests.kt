package no.nav.tsm.sykinnapi.health

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
class ApplicationHealthTests {

    @Test
    internal fun `Should return HttpStatus OK when calling endpoint interal health`(
        @Autowired webClient: WebTestClient
    ) {
        webClient.get().uri("/internal/health").exchange().expectStatus().isOk()
    }
}
