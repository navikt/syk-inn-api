package no.nav.tsm.core

import org.slf4j.Logger
import org.slf4j.LoggerFactory

fun logger(): Logger =
    LoggerFactory.getLogger(
        StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).callerClass
    )

fun teamLogger(): Logger =
    LoggerFactory.getLogger(
        "teamlog.${StackWalker.getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE).callerClass}"
    )
