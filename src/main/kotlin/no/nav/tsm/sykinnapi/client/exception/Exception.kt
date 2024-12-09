package no.nav.tsm.sykinnapi.client.exception

import java.net.URI
import org.springframework.http.HttpStatus.resolve
import org.springframework.http.HttpStatusCode
import org.springframework.http.ProblemDetail.forStatusAndDetail
import org.springframework.web.ErrorResponseException

open class IntegrationException(
    status: HttpStatusCode,
    uri: URI,
    detail: String? = null,
    cause: Throwable? = null,
    stackTrace: String? = null,
    validationErrors: List<String>? = emptyList()
) : ErrorResponseException(status, pd(status, detail, uri, stackTrace, validationErrors), cause) {
    companion object {
        private fun pd(
            status: HttpStatusCode,
            detail: String?,
            uri: URI,
            stackTrace: String? = null,
            validationErrors: List<String>? = emptyList()
        ) =
            forStatusAndDetail(status, detail).apply {
                title = resolve(status.value())?.reasonPhrase ?: "$status"
                setProperty("uri", uri)
                validationErrors?.isNotEmpty().let {
                    setProperty("validationErrors", validationErrors)
                }
                stackTrace?.let { setProperty("stackTrace", it) }
            }
    }
}

class IrrecoverableIntegrationException(
    status: HttpStatusCode,
    uri: URI,
    detail: String? = null,
    cause: Throwable? = null,
    stackTrace: String? = null,
    validationErrors: List<String>? = emptyList()
) : IntegrationException(status, uri, detail, cause, stackTrace, validationErrors)

class RecoverableIntegrationException(
    status: HttpStatusCode,
    uri: URI,
    detail: String? = "Fikk respons $status",
    cause: Throwable? = null
) : IntegrationException(status, uri, detail, cause)
