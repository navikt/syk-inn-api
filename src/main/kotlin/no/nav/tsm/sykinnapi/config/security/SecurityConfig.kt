package no.nav.tsm.sykinnapi.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            csrf { disable() }
            oauth2ResourceServer { jwt {} }
            oauth2Client {}
            authorizeHttpRequests {
                authorize("/internal/**", permitAll)
                authorize("/v3/api-docs/**", permitAll)
                authorize("/api/**", authenticated)
                authorize(anyRequest, denyAll)
            }
            cors { disable() }
        }
        return http.build()
    }
}
