package no.nav.tsm.core.otel

import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode
import no.nav.tsm.core.logger

private val log = logger()

fun failSpan(span: Span, exception: Throwable): Throwable {
    span.setStatus(StatusCode.ERROR)
    span.recordException(exception)
    return exception
}

fun Throwable.failSpan(): Throwable {
    val span = Span.current()

    return failSpan(span, this)
}

fun Throwable.failSpan(span: Span): Throwable {
    return failSpan(span, this)
}
