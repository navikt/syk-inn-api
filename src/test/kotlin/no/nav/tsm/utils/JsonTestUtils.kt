package no.nav.tsm.utils

import java.util.UUID
import tools.jackson.module.kotlin.jacksonMapperBuilder

/**
 * This should ONLY be used to prepare JSON for tests, this will write JSON as normal JSON where
 * dates are ISO8601 strings etc.
 */
val testJsonObjectMapper = jacksonMapperBuilder().build()

fun String.uuid(): UUID = UUID.fromString(this)
