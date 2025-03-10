package no.nav.tsm.sykinnapi.client

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.web.client.RestClient

@Configuration
@Import(OAuth2ClientAutoConfiguration::class)
class RestClientConfiguration {

    @Bean("syfosmreglerClientRestClient")
    fun syfosmreglerClient(
        @Value("\${syfosmregler.url}") syfosmreglerBaseUrl: String,
        syfosmreglerM2mRestClientBuilder: RestClient.Builder
    ): RestClient {
        return syfosmreglerM2mRestClientBuilder.baseUrl(syfosmreglerBaseUrl).build()
    }

    @Bean("syfohelsenettproxyClientRestClient")
    fun syfohelsenettproxyClient(
        @Value("\${syfohelsenettproxy.url}") syfohelsenettproxyBaseUrl: String,
        syfohelsenettproxyM2mRestClientBuilder: RestClient.Builder
    ): RestClient {
        return syfohelsenettproxyM2mRestClientBuilder.baseUrl(syfohelsenettproxyBaseUrl).build()
    }

    @Bean("syfosmregisterClientRestClient")
    fun syfosmregisterRestClient(
        @Value("\${syfosmregister.url}") syfosmregisterBaseUrl: String,
        syfosmregisterM2mRestClientBuilder: RestClient.Builder
    ): RestClient {
        return syfosmregisterM2mRestClientBuilder.baseUrl(syfosmregisterBaseUrl).build()
    }

    @Bean("smpdfgenClientRestClient")
    fun smpdfgenClientRestClient(
        @Value("\${smpdfgen.url}") smpdfgenBaseUrl: String,
        smpdfgenM2mRestClientBuilder: RestClient.Builder
    ): RestClient {
        return smpdfgenM2mRestClientBuilder.baseUrl(smpdfgenBaseUrl).build()
    }

    @Bean("tsmPdlClientRestClient")
    fun tsmPdlClientRestClient(
        @Value("\${tsmpdl.url}") tsmpdlBaseUrl: String,
        tsmpdlM2mRestClientBuilder: RestClient.Builder
    ): RestClient {
        return tsmpdlM2mRestClientBuilder.baseUrl(tsmpdlBaseUrl).build()
    }

    @Bean
    fun restClientBuilderCustomizer(acm: OAuth2AuthorizedClientManager) = RestClientCustomizer {
        it.requestInterceptor(OAuth2ClientHttpRequestInterceptor(acm))
    }
}
