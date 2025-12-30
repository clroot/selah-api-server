package io.clroot.selah.domains.prayer.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId

/**
 * 기도제목 응답 관련 UseCase
 */
interface AnswerPrayerTopicUseCase {
    /**
     * 기도제목을 응답 상태로 변경합니다.
     */
    suspend fun markAsAnswered(command: MarkAsAnsweredCommand): PrayerTopic

    /**
     * 응답 상태를 취소합니다.
     */
    suspend fun cancelAnswer(command: CancelAnswerCommand): PrayerTopic

    /**
     * 응답된 기도제목의 소감을 수정합니다.
     */
    suspend fun updateReflection(command: UpdateReflectionCommand): PrayerTopic
}

/**
 * 응답 체크 커맨드
 */
data class MarkAsAnsweredCommand(
    val id: PrayerTopicId,
    val memberId: MemberId,
    val reflection: String?,
)

/**
 * 응답 취소 커맨드
 */
data class CancelAnswerCommand(
    val id: PrayerTopicId,
    val memberId: MemberId,
)

/**
 * 소감 수정 커맨드
 */
data class UpdateReflectionCommand(
    val id: PrayerTopicId,
    val memberId: MemberId,
    val reflection: String?,
)
