package no.nav.tsm.utils

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.util.UUID

/**
 * This should ONLY be used to prepare JSON for tests, this will write JSON as normal JSON where
 * dates are ISO8601 strings etc.
 */
val testJsonObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())

        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }

fun String.uuid(): UUID = UUID.fromString(this)
