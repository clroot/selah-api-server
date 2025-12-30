package io.clroot.selah.domains.prayer.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.response.ErrorResponse
import io.clroot.selah.domains.prayer.domain.exception.PrayerException
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicAccessDeniedException
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

/**
 * Prayer 도메인 예외 핸들러
 */
@RestControllerAdvice(basePackages = ["io.clroot.selah.domains.prayer"])
class PrayerExceptionHandler {

    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    @ExceptionHandler(PrayerException::class)
    fun handlePrayerException(ex: PrayerException): ResponseEntity<ApiResponse<Nothing>> {
        logger.debug { "Prayer exception: ${ex.code} - ${ex.message}" }

        val (status, message) = when (ex) {
            is PrayerTopicNotFoundException -> HttpStatus.NOT_FOUND to "기도제목을 찾을 수 없습니다"
            is PrayerTopicAccessDeniedException -> HttpStatus.FORBIDDEN to "해당 기도제목에 접근할 수 없습니다"
        }

        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(ErrorResponse(ex.code, message)))
    }
}
