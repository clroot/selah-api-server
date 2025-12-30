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

/**
 * 기도문을 찾을 수 없는 경우
 */
class PrayerNotFoundException(id: String) : PrayerException(
    code = "PRAYER_NOT_FOUND",
    message = "Prayer not found: $id",
)

/**
 * 기도문에 대한 접근 권한이 없는 경우
 */
class PrayerAccessDeniedException(id: String) : PrayerException(
    code = "PRAYER_ACCESS_DENIED",
    message = "Access denied to prayer: $id",
)

/**
 * 이미 응답된 기도제목인 경우
 */
class PrayerTopicAlreadyAnsweredException(id: String) : PrayerException(
    code = "PRAYER_TOPIC_ALREADY_ANSWERED",
    message = "Prayer topic already answered: $id",
)

/**
 * 응답 상태가 아닌 기도제목인 경우
 */
class PrayerTopicNotAnsweredException(id: String) : PrayerException(
    code = "PRAYER_TOPIC_NOT_ANSWERED",
    message = "Prayer topic not answered: $id",
)
