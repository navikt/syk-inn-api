package no.nav.tsm.syk_inn_api.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.json.JsonMapper
import tools.jackson.module.kotlin.kotlinModule

@Configuration
class JacksonConfig {

    /**
     * Configures Jackson 3 ObjectMapper with Kotlin module support. This ensures proper
     * serialization/deserialization of Kotlin data classes, including handling of non-null
     * parameters, default values, and sealed classes.
     */
    @Bean
    fun objectMapper(): ObjectMapper {
        return JsonMapper.builder().addModule(kotlinModule()).build()
    }
}
