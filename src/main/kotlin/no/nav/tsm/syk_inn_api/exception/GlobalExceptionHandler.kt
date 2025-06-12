package no.nav.tsm.syk_inn_api.exception

import jakarta.servlet.http.HttpServletRequest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import no.nav.tsm.syk_inn_api.utils.logger
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = logger()

    /** Handles errors when consumers forget to include required headers in their requests. */
    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(
        ex: MissingRequestHeaderException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val error =
            ApiError(
                message = "Missing required header: '${ex.headerName}'",
                path = request.requestURI,
            )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    /** Handles cases where the request body is not readable, such as when the JSON is malformed. */
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val errorMessage =
            "Invalid or missing request body. Please ensure the payload is properly formatted."

        logger.error(
            "HttpMessageNotReadableException while processing request to ${request.requestURI}: ${ex.message}",
            ex,
        )

        val error =
            ApiError(
                message = errorMessage + " Error: ${ex.message}" + "e: $ex",
                path = request.requestURI,
            )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    /** Catch all for any unexpected (non-logical failures) that happens anywhere in the app. */
    @ExceptionHandler(Throwable::class)
    fun handleGeneralException(ex: Throwable, request: HttpServletRequest): ResponseEntity<Any> {
        logger.error(
            "Unhandled exception while processing request to ${request.requestURI}: ${ex.message}",
            ex,
        )

        if (request.getHeader("Accept") == "application/pdf") {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.TEXT_PLAIN)
                .body("Failed to generate PDF, unexpected error.")
        }

        val error =
            ApiError(
                message = "An unexpected error occurred. Please try again later.",
                path = request.requestURI,
            )

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error)
    }
}

data class ApiError(
    val message: String,
    val path: String,
    val timestamp: String = ZonedDateTime.now(ZoneOffset.UTC).toString()
)
