package no.nav.tsm.syk_inn_api.utils

import org.slf4j.Logger
import org.slf4j.LoggerFactory

inline fun <reified T> T.logger(): Logger = LoggerFactory.getLogger(T::class.java)

inline fun <reified T> T.teamLogger(): Logger =
    LoggerFactory.getLogger("teamlog.${T::class.java.name}")
