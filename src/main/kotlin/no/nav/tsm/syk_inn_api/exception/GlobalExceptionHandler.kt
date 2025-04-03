package no.nav.tsm.syk_inn_api.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.support.MethodArgumentTypeMismatchException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

@RestControllerAdvice
class GlobalExceptionHandler {
    private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    private val securelog = LoggerFactory.getLogger("securelog")

    @ExceptionHandler
    fun handleIllegalArgumentException(
        iae: IllegalArgumentException
    ): ResponseEntity<ErrorMessage> {

        val errorMessage = ErrorMessage(HttpStatus.BAD_REQUEST.value(), iae.message)
        return ResponseEntity(errorMessage, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleInvalidUUID(ex: MethodArgumentTypeMismatchException): ResponseEntity<ErrorMessage> {
        val errorMessage =
            ErrorMessage(
                status = HttpStatus.BAD_REQUEST.value(),
                message = "Invalid UUID format for parameter '${ex.message}'"
            )
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
}

data class ErrorMessage(var status: Int? = null, var message: String? = null)

class BehandlerNotFoundException(message: String) : RuntimeException(message)

class PdlException(message: String) : RuntimeException(message)

class BtsysException(message: String) : RuntimeException(message)

class HelsenettProxyException(message: String) : RuntimeException(message)
