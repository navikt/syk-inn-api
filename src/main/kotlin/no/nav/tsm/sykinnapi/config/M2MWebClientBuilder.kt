package no.nav.tsm.sykinnapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.WebClient

@Configuration
class M2MWebClientBuilder(
    private val webClientBuilder: WebClient.Builder,
    private val m2mTokenService: M2MTokenService,
) {
    @Bean
    fun syfohelsenettproxyM2mWebBuilder(): WebClient {
        return webClientBuilder
            .defaultHeader(
                HttpHeaders.AUTHORIZATION,
                "Bearer ${m2mTokenService.getM2MToken("syfohelsenettproxy-m2m")}"
            )
            .build()
    }
}
