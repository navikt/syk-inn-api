import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter
import no.nav.tsm.syk_inn_api.model.Sykmelding
import org.slf4j.LoggerFactory

@Converter(autoApply = true)
class SykmeldingConverter : AttributeConverter<Sykmelding, String> {
    private val objectMapper =
        ObjectMapper().apply {
            registerModule(JavaTimeModule())
            registerModule(KotlinModule.Builder().build())
        }
    private val logger = LoggerFactory.getLogger(SykmeldingConverter::class.java)

    override fun convertToDatabaseColumn(attribute: Sykmelding?): String? {
        return try {
            logger.debug("Converting Sykmelding to JSON string for database storage")
            attribute?.let { objectMapper.writeValueAsString(it) }
        } catch (e: Exception) {
            logger.error("Error converting Sykmelding to JSON string: ${e.message}", e)
            throw IllegalArgumentException("Could not convert Sykmelding to JSON string", e)
        }
    }

    override fun convertToEntityAttribute(dbData: String?): Sykmelding? {
        return try {
            logger.debug("Converting JSON string from database to Sykmelding")
            dbData?.let { objectMapper.readValue(it, Sykmelding::class.java) }
        } catch (e: Exception) {
            logger.error("Error converting JSON string to Sykmelding: ${e.message}", e)
            throw IllegalArgumentException("Could not convert JSON string to Sykmelding", e)
        }
    }
}
