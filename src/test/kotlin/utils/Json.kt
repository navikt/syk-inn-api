package utils

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue

val testJacksonMapper = jacksonObjectMapper()

inline fun <reified Type> ByteArray.parse() = testJacksonMapper.readValue<Type>(this)
