package no.nav.tsm.sykinnapi.client

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.security.oauth2.client.servlet.OAuth2ClientAutoConfiguration
import org.springframework.boot.web.client.RestClientCustomizer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpRequest
import org.springframework.http.client.ClientHttpResponse
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager
import org.springframework.security.oauth2.client.web.client.OAuth2ClientHttpRequestInterceptor
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient.ResponseSpec.ErrorHandler

@Import(OAuth2ClientAutoConfiguration::class)
class RestClientConfiguration {

    @Component
    class LoggingErrorHandler : ErrorHandler {
        private val logger = LoggerFactory.getLogger(LoggingErrorHandler::class.java)

        override fun handle(request: HttpRequest, response: ClientHttpResponse) {
            throw RuntimeException(
                    "Feil ved henting av behandlerMedHprNummer: ${response.statusCode}"
                )
                .also { logger.error(it.message, it) }
        }
    }

    @Bean
    fun restClientBuilderCustomizer(acm: OAuth2AuthorizedClientManager) = RestClientCustomizer {
        it.requestInterceptor(OAuth2ClientHttpRequestInterceptor(acm))
    }
}
