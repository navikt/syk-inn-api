package no.nav.tsm.sykinnapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class M2MWebClientBuilder(
    private val webClientBuilder: WebClient.Builder,
) {
    @Bean
    fun syfohelsenettproxyM2mWebBuilder(): WebClient {
        return webClientBuilder.build()
    }
}
