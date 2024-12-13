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

@Import(OAuth2ClientAutoConfiguration::class)
class RestClientConfiguration {

    @Component
    class LoggingErrorHandler : ErrorHandler {
        private val logger = LoggerFactory.getLogger(LoggingErrorHandler::class.java)

        override fun handle(request: HttpRequest, response: ClientHttpResponse) {
            throw RuntimeException("Error got statuscode: ${response.statusCode}").also {
                logger.error(it.message, it)
            }
        }
    }

    @Bean
    fun restClientBuilderCustomizer(acm: OAuth2AuthorizedClientManager) = RestClientCustomizer {
        it.requestInterceptor(OAuth2ClientHttpRequestInterceptor(acm))
    }
}
