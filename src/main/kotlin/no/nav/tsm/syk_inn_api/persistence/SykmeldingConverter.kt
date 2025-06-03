package no.nav.tsm.syk_inn_api.persistence

import com.fasterxml.jackson.databind.ObjectMapper
import org.postgresql.util.PGobject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions

@WritingConverter
class SykmeldingWritingConverter(private val objectMapper: ObjectMapper = ObjectMapper()) :
    Converter<PersistedSykmelding, PGobject> {

    override fun convert(source: PersistedSykmelding): PGobject {
        val json = objectMapper.writeValueAsString(source)
        return PGobject().apply {
            type = "jsonb"
            value = json
        }
    }
}

@ReadingConverter
class SykmeldingReadingConverter(private val objectMapper: ObjectMapper = ObjectMapper()) :
    Converter<Any, PersistedSykmelding> {

    override fun convert(source: Any): PersistedSykmelding {
        val json =
            when (source) {
                is PGobject -> source.value
                is String -> source
                else ->
                    throw IllegalArgumentException(
                        "Unsupported type for Sykmelding: ${source.javaClass}"
                    )
            }
        return objectMapper.readValue(json, PersistedSykmelding::class.java)
    }
}

@Configuration
class CustomConverterConfig {
    @Bean
    fun customConversions(): JdbcCustomConversions {
        println("Registering custom converters for Sykmelding persistence")
        return JdbcCustomConversions(
            listOf(SykmeldingWritingConverter(), SykmeldingReadingConverter())
        )
    }
}

