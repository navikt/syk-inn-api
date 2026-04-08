package no.nav.tsm.modules.sykmeldinger.db.exposed

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

val exposedJacksonObjectMapper =
    jacksonObjectMapper().apply {
        registerModule(JavaTimeModule())

        configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
    }
