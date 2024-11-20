package no.nav.tsm.sykinnapi.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.web.reactive.function.client.ClientRequest
import org.springframework.web.reactive.function.client.ExchangeFilterFunction
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

@Configuration
class M2MWebClientBuilder(
    private val webClientBuilder: WebClient.Builder,
    private val m2mTokenService: M2MTokenService
) {
    @Bean
    fun syfohelsenettproxyM2mWebBuilder(): WebClient {
        return webClientBuilder
            .filter(
                ExchangeFilterFunction.ofRequestProcessor { request ->
                    Mono.just(
                        ClientRequest.from(request)
                            .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer ${m2mTokenService.getM2MToken("syfohelsenettproxy-m2m")}",
                            )
                            .build(),
                    )
                },
            )
            .build()
    }
}
