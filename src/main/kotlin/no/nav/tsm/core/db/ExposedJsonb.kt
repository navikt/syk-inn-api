package no.nav.tsm.core.db

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.jetbrains.exposed.v1.core.Column
import org.jetbrains.exposed.v1.core.Table
import org.jetbrains.exposed.v1.json.jsonb

val exposedJacksonObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())

        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

inline fun <reified Type : Any> Table.jacksonJsonb(name: String): Column<Type> {
    val writer = exposedJacksonObjectMapper.writerFor(object : TypeReference<Type>() {})

    return jsonb(
        name,
        { writer.writeValueAsString(it) },
        { exposedJacksonObjectMapper.readValue<Type>(it) },
    )
}
