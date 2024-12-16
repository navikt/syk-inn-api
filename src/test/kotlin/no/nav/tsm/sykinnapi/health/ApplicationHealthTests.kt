package no.nav.tsm.sykinnapi.health

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
class ApplicationHealthTests(
    @Value("\${management.endpoints.web.base-path}") private val basePath: String
) {
    @Autowired private lateinit var mockMvc: MockMvc
    @MockkBean private lateinit var restClientBuilderCustomizer: OAuth2AuthorizedClientManager
    @MockkBean private lateinit var securityFilterChain: ClientRegistrationRepository

    @Test
    internal fun `Should return HttpStatus OK when calling endpoint internal health`() {
        mockMvc
            .perform(get("$basePath/health").accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
    }
}
