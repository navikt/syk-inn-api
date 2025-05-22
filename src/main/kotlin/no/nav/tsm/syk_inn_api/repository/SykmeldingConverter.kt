import com.fasterxml.jackson.databind.ObjectMapper
import no.nav.tsm.syk_inn_api.model.sykmelding.Sykmelding
import org.postgresql.util.PGobject
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions

@WritingConverter
class SykmeldingWritingConverter(private val objectMapper: ObjectMapper = ObjectMapper()) :
    Converter<Sykmelding, PGobject> {

    override fun convert(source: Sykmelding): PGobject {
        val json = objectMapper.writeValueAsString(source)
        return PGobject().apply {
            type = "jsonb"
            value = json
        }
    }
}

@ReadingConverter
class SykmeldingReadingConverter(private val objectMapper: ObjectMapper = ObjectMapper()) :
    Converter<Any, Sykmelding> {

    override fun convert(source: Any): Sykmelding {
        val json =
            when (source) {
                is PGobject -> source.value
                is String -> source
                else ->
                    throw IllegalArgumentException(
                        "Unsupported type for Sykmelding: ${source.javaClass}"
                    )
            }
        return objectMapper.readValue(json, Sykmelding::class.java)
    }
}

@Configuration
class CustomConverterConfig {
    @Bean
    fun customConversions(): JdbcCustomConversions {
        return JdbcCustomConversions(
            listOf(SykmeldingWritingConverter(), SykmeldingReadingConverter())
        )
    }
}
