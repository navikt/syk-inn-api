package no.nav.tsm.syk_inn_api.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Profile
import org.springframework.security.config.Customizer
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.annotation.web.invoke
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    @Profile("local", "test")
    fun localSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            //            .authorizeHttpRequests { it.anyRequest().authenticated() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .httpBasic(Customizer.withDefaults())
        return http.build()
    }

    @Bean
    @Profile("default")
    fun defaultSecurityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http {
            securityMatcher("/**")
            csrf { disable() }
            oauth2ResourceServer { jwt {} }
            oauth2Client {}
            authorizeHttpRequests {
                authorize("/internal/**", permitAll)
                authorize(anyRequest, authenticated)
            }
            cors { disable() }
        }
        return http.build()
    }
}
