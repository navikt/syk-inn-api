package no.nav.tsm.syk_inn_api.utils

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

fun failSpan(span: Span, exception: Throwable): Throwable {
    span.setStatus(StatusCode.ERROR)
    span.recordException(exception)
    return exception
}

fun failSpan(exception: Throwable): Throwable {
    val span = Span.current()

    return failSpan(span, exception)
}
