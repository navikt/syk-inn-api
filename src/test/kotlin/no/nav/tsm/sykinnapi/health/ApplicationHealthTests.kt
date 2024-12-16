package no.nav.tsm.sykinnapi.health

import com.ninjasquad.springmockk.MockkBean
import no.nav.tsm.sykinnapi.client.syfohelsenettproxy.SyfohelsenettproxyClient
import no.nav.tsm.sykinnapi.client.syfosmregler.SyfosmreglerClient
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType.APPLICATION_JSON
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.web.client.RestClient

@SpringBootTest
@AutoConfigureMockMvc
@Import(OAuth2ClientAutoConfiguration::class)
class ApplicationHealthTests(
    @Value("\${management.endpoints.web.base-path}") private val basePath: String
) {

    @Autowired private lateinit var mockMvc: MockMvc

    @MockkBean private lateinit var syfohelsenettproxyClient: SyfohelsenettproxyClient
    @MockkBean(name = "syfohelsenettproxyClientRestClient")
    private lateinit var syfohelsenettproxyClientRestClient: RestClient

    @MockkBean(name = "syfosmreglerClientRestClient")
    private lateinit var syfosmreglerClientRestClient: RestClient
    @MockkBean private lateinit var syfosmreglerClient: SyfosmreglerClient
    @MockkBean private lateinit var restClientBuilderCustomizer: OAuth2AuthorizedClientManager

    @Test
    internal fun `Should return HttpStatus OK when calling endpoint internal health`() {
        mockMvc
            .perform(get("$basePath/health").accept(APPLICATION_JSON))
            .andExpect(status().isOk)
            .andExpect(content().contentType(APPLICATION_JSON))
    }
}
