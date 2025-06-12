package no.nav.tsm.syk_inn_api.exception

import jakarta.servlet.http.HttpServletRequest
import java.time.ZoneOffset
import java.time.ZonedDateTime
import no.nav.tsm.syk_inn_api.utils.logger
import no.nav.tsm.syk_inn_api.utils.secureLogger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.MissingRequestHeaderException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = logger()
    private val securelog = secureLogger()

    @ExceptionHandler
    fun handleIllegalArgumentException(
        iae: IllegalArgumentException
    ): ResponseEntity<ErrorMessage> {

        logger.error("IllegalArgumentException occurred ${iae.message}", iae)

        val errorMessage = ErrorMessage(HttpStatus.BAD_REQUEST.value(), iae.message)
        return ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(BehandlerNotFoundException::class)
    fun handleBehandlerNotFoundException(
        ex: BehandlerNotFoundException
    ): ResponseEntity<ErrorMessage> {
        val errorMessage = ErrorMessage(status = HttpStatus.NOT_FOUND.value(), message = ex.message)
        return ResponseEntity(errorMessage, HttpStatus.NOT_FOUND)
    }

    @ExceptionHandler(PdlException::class)
    fun handlePdlException(ex: PdlException): ResponseEntity<ErrorMessage> {
        val errorMessage =
            ErrorMessage(status = HttpStatus.INTERNAL_SERVER_ERROR.value(), message = ex.message)
        return ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(BtsysException::class)
    fun handleBtsysException(ex: BtsysException): ResponseEntity<ErrorMessage> {
        val errorMessage =
            ErrorMessage(status = HttpStatus.INTERNAL_SERVER_ERROR.value(), message = ex.message)
        return ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(HelsenettProxyException::class)
    fun handleHelsenettProxyException(ex: HelsenettProxyException): ResponseEntity<ErrorMessage> {
        val errorMessage =
            ErrorMessage(status = HttpStatus.INTERNAL_SERVER_ERROR.value(), message = ex.message)
        return ResponseEntity(errorMessage, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(RuleHitException::class)
    fun handleRuleHitException(ex: RuleHitException): ResponseEntity<ErrorMessage> {
        val errorMessage =
            ErrorMessage(status = HttpStatus.BAD_REQUEST.value(), message = ex.message)
        return ResponseEntity(
            errorMessage,
            HttpStatus.BAD_REQUEST
        ) // TODO status code should we use ?
    }

    @ExceptionHandler(MissingRequestHeaderException::class)
    fun handleMissingHeader(
        ex: MissingRequestHeaderException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val error =
            ApiError(
                message = "Missing required header: '${ex.headerName}'",
                status = HttpStatus.BAD_REQUEST.value(),
                path = request.requestURI
            )
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(
        ex: HttpMessageNotReadableException,
        request: HttpServletRequest
    ): ResponseEntity<ApiError> {
        val errorMessage =
            "Invalid or missing request body. Please ensure the payload is properly formatted."

        logger.error(
            "HttpMessageNotReadableException while processing request to ${request.requestURI}: ${ex.message}",
            ex
        )

        val error =
            ApiError(
                message = errorMessage + " Error: ${ex.message}" + "e: $ex",
                status = HttpStatus.BAD_REQUEST.value(),
                path = request.requestURI
            )

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error)
    }
}

data class ErrorMessage(var status: Int? = null, var message: String? = null)

class BehandlerNotFoundException(message: String) : RuntimeException(message)

class PdlException(message: String) : RuntimeException(message)

class BtsysException(message: String) : RuntimeException(message)

class HelsenettProxyException(message: String) : RuntimeException(message)

class RuleHitException(message: String) : RuntimeException(message)

class PersonNotFoundException(message: String) : Exception(message)

class SykmeldingDBMappingException(message: String, ex: Exception) : Exception(message, ex)

data class ApiError(
    val message: String,
    val status: Int,
    val path: String,
    val timestamp: String = ZonedDateTime.now(ZoneOffset.UTC).toString()
)
