package no.nav.tsm.sykinnapi.config.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.SecurityFilterChain

@Configuration
class SecurityConfig {

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain? {
        return http
            .authorizeHttpRequests { authorizeRequests ->
                authorizeRequests
                    .requestMatchers(HttpMethod.GET, "/internal/**")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/v3/api-docs/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated()
            }
            .headers { headersConfigurer ->
                headersConfigurer.frameOptions { frameOptionsCustomizer ->
                    frameOptionsCustomizer.sameOrigin()
                }
            }
            .build()
    }
}
