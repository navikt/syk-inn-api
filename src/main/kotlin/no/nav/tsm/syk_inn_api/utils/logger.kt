// src/main/kotlin/com/example/util/Logging.kt
package com.example.util

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

inline fun <reified T> T.secureLogger(): Logger =
    LoggerFactory.getLogger("securelog.${T::class.java.name}")
