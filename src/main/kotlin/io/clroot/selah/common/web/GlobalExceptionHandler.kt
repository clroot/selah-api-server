package io.clroot.selah.common.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.response.ErrorResponse
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException

/**
 * 전역 예외 핸들러
 *
 * 도메인별 예외 핸들러에서 처리되지 않은 예외를 처리합니다.
 * @Order를 지정하지 않아 도메인 핸들러보다 낮은 우선순위를 가집니다.
 */
@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    // ========== 400 Bad Request ==========

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug { "Illegal argument: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorResponse("INVALID_ARGUMENT", "잘못된 요청입니다")))
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException): ResponseEntity<ApiResponse<Nothing>> {
        val errors = ex.bindingResult.fieldErrors
            .map { "${it.field}: ${it.defaultMessage}" }
            .joinToString(", ")
        logger.debug { "Validation failed: $errors" }

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorResponse("VALIDATION_FAILED", "입력값이 올바르지 않습니다")))
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug { "Message not readable: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorResponse("MALFORMED_REQUEST", "요청 본문을 읽을 수 없습니다")))
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingServletRequestParameter(
        ex: MissingServletRequestParameterException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug { "Missing parameter: ${ex.parameterName}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorResponse("MISSING_PARAMETER", "필수 파라미터가 누락되었습니다")))
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(
        ex: MethodArgumentTypeMismatchException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug { "Type mismatch: ${ex.name} = ${ex.value}" }
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorResponse("TYPE_MISMATCH", "파라미터 타입이 올바르지 않습니다")))
    }

    // ========== 403 Forbidden ==========

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDenied(ex: AccessDeniedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug { "Access denied: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ErrorResponse("ACCESS_DENIED", "접근 권한이 없습니다")))
    }

    // ========== 404 Not Found ==========

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFound(ex: NoHandlerFoundException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug { "No handler found: ${ex.httpMethod} ${ex.requestURL}" }
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ErrorResponse("NOT_FOUND", "요청한 리소스를 찾을 수 없습니다")))
    }

    // ========== 405 Method Not Allowed ==========

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleHttpRequestMethodNotSupported(
        ex: HttpRequestMethodNotSupportedException,
    ): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug { "Method not supported: ${ex.method}" }
        return ResponseEntity
            .status(HttpStatus.METHOD_NOT_ALLOWED)
            .body(ApiResponse.error(ErrorResponse("METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다")))
    }

    // ========== 500 Internal Server Error ==========

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ApiResponse<Nothing>> {
        logger.error(ex) { "Unhandled exception: ${ex.message}" }
        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(ApiResponse.error(ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다")))
    }
}
