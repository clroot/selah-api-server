package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.response.ErrorResponse
import io.clroot.selah.domains.member.domain.exception.*
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Member 도메인 예외 처리기
 */
@RestControllerAdvice
class MemberExceptionHandler {

    private val logger = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(EmailAlreadyExistsException::class)
    fun handleEmailAlreadyExists(ex: EmailAlreadyExistsException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("Email already exists: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ErrorResponse(ex.code, ex.message)))
    }

    @ExceptionHandler(MemberNotFoundException::class)
    fun handleMemberNotFound(ex: MemberNotFoundException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("Member not found: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ErrorResponse(ex.code, ex.message)))
    }

    @ExceptionHandler(InvalidCredentialsException::class)
    fun handleInvalidCredentials(ex: InvalidCredentialsException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("Invalid credentials")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ErrorResponse(ex.code, ex.message)))
    }

    @ExceptionHandler(EmailNotVerifiedException::class)
    fun handleEmailNotVerified(ex: EmailNotVerifiedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("Email not verified: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .body(ApiResponse.error(ErrorResponse(ex.code, ex.message)))
    }

    @ExceptionHandler(SessionExpiredException::class)
    fun handleSessionExpired(ex: SessionExpiredException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("Session expired")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ErrorResponse(ex.code, ex.message)))
    }

    @ExceptionHandler(InvalidSessionException::class)
    fun handleInvalidSession(ex: InvalidSessionException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("Invalid session")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ErrorResponse(ex.code, ex.message)))
    }

    @ExceptionHandler(InvalidApiKeyException::class)
    fun handleInvalidApiKey(ex: InvalidApiKeyException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("Invalid API key")
        return ResponseEntity
            .status(HttpStatus.UNAUTHORIZED)
            .body(ApiResponse.error(ErrorResponse(ex.code, ex.message)))
    }

    @ExceptionHandler(OAuthProviderAlreadyConnectedException::class)
    fun handleOAuthProviderAlreadyConnected(ex: OAuthProviderAlreadyConnectedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("OAuth provider already connected: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiResponse.error(ErrorResponse(ex.code, ex.message)))
    }

    @ExceptionHandler(OAuthProviderNotConnectedException::class)
    fun handleOAuthProviderNotConnected(ex: OAuthProviderNotConnectedException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("OAuth provider not connected: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(ApiResponse.error(ErrorResponse(ex.code, ex.message)))
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(ex: IllegalArgumentException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug("Invalid argument: {}", ex.message)
        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(ApiResponse.error(ErrorResponse("INVALID_ARGUMENT", ex.message ?: "Invalid argument")))
    }
}
