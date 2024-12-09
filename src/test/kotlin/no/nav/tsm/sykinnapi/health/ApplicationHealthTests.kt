package no.nav.tsm.sykinnapi.health

import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient
import org.springframework.boot.test.autoconfigure.web.client.RestClientTest
import org.springframework.http.HttpStatus
import org.springframework.web.client.RestClient

// @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@RestClientTest
@AutoConfigureWebClient(registerRestTemplate = true)
class ApplicationHealthTests {

    @Test
    internal fun `Should return HttpStatus OK when calling endpoint internal health`(
        @Autowired restClient: RestClient
    ) {
        val entity = restClient.get().uri("/internal/health").retrieve().toBodilessEntity()
        assertEquals(HttpStatus.OK, entity.statusCode)
    }
}
