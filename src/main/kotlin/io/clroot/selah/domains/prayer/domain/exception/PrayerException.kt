package io.clroot.selah.domains.prayer.domain.exception

/**
 * Prayer 도메인 예외의 기본 클래스
 */
sealed class PrayerException(
    val code: String,
    override val message: String,
) : RuntimeException(message)

/**
 * 기도제목을 찾을 수 없는 경우
 */
class PrayerTopicNotFoundException(id: String) : PrayerException(
    code = "PRAYER_TOPIC_NOT_FOUND",
    message = "Prayer topic not found: $id",
)

/**
 * 기도제목에 대한 접근 권한이 없는 경우
 */
class PrayerTopicAccessDeniedException(id: String) : PrayerException(
    code = "PRAYER_TOPIC_ACCESS_DENIED",
    message = "Access denied to prayer topic: $id",
)
