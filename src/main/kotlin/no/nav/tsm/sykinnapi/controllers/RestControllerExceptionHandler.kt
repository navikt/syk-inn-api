package no.nav.tsm.sykinnapi.controllers

import java.time.LocalDateTime
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.ServletWebRequest

@ControllerAdvice(annotations = [RestController::class])
class RestControllerExceptionHandler {

    private val logger = LoggerFactory.getLogger(RestControllerExceptionHandler::class.java)
    private val securelog = LoggerFactory.getLogger("securelog")

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(
        illegalArgumentException: IllegalArgumentException,
        servletWebRequest: ServletWebRequest
    ): Any {
        return ResponseEntity.badRequest()
            .body<Map<String, Any?>>(
                createErrorBody(
                    HttpStatus.BAD_REQUEST,
                    illegalArgumentException,
                    servletWebRequest,
                ),
            )
    }

    @ExceptionHandler(RuntimeException::class)
    fun handleRuntimeException(
        runtimeException: RuntimeException,
        servletWebRequest: ServletWebRequest
    ): Any {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body<Map<String, Any?>>(
                createErrorBody(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    runtimeException,
                    servletWebRequest,
                ),
            )
    }

    private fun createErrorBody(
        status: HttpStatus,
        ex: RuntimeException,
        req: ServletWebRequest
    ): MutableMap<String, Any?> {
        val body: MutableMap<String, Any?> = LinkedHashMap()
        body["timestamp"] = LocalDateTime.now().toString()
        body["status"] = status.value()
        body["error"] = status.reasonPhrase
        body["type"] = ex.javaClass.simpleName
        body["path"] = req.request.requestURI
        logger.error("An error has occurred $body")
        body["message"] = ex.message
        securelog.error("An error has occurred $body", ex)
        return body
    }
}
