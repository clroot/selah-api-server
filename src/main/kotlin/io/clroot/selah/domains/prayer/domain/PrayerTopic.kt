package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.common.domain.AggregateRoot
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicAlreadyAnsweredException
import io.clroot.selah.domains.prayer.domain.exception.PrayerTopicNotAnsweredException
import java.time.LocalDateTime

/**
 * PrayerTopic Aggregate Root
 *
 * 기도제목을 관리합니다.
 * title과 reflection은 E2E 암호화된 암호문(Base64)으로 저장됩니다.
 */
class PrayerTopic(
    override val id: PrayerTopicId,
    // --- 비즈니스 필드 ---
    val memberId: MemberId,
    title: String,
    status: PrayerTopicStatus,
    answeredAt: LocalDateTime?,
    reflection: String?,
    // --- 메타 필드 (하단) ---
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<PrayerTopicId>(id, version, createdAt, updatedAt) {
    init {
        require(title.isNotBlank()) { "Title cannot be blank" }
    }

    var title: String = title
        private set

    var status: PrayerTopicStatus = status
        private set

    var answeredAt: LocalDateTime? = answeredAt
        private set

    var reflection: String? = reflection
        private set

    /**
     * 기도제목(title)을 수정합니다.
     * title은 암호문(Base64)입니다.
     */
    fun updateTitle(newTitle: String) {
        require(newTitle.isNotBlank()) { "Title cannot be blank" }
        if (title != newTitle) {
            title = newTitle
            touch()
        }
    }

    /**
     * 기도제목을 응답 상태로 변경합니다.
     *
     * @param reflection 응답 소감 (암호문, 선택)
     * @throws IllegalStateException 이미 응답된 기도제목인 경우
     */
    fun markAsAnswered(reflection: String? = null) {
        if (status == PrayerTopicStatus.ANSWERED) {
            throw PrayerTopicAlreadyAnsweredException(id.value)
        }
        check(status == PrayerTopicStatus.PRAYING) {
            "이미 응답된 기도제목입니다"
        }
        this.status = PrayerTopicStatus.ANSWERED
        this.answeredAt = LocalDateTime.now()
        this.reflection = reflection
        touch()
    }

    /**
     * 응답 상태를 취소하고 기도 중 상태로 되돌립니다.
     * 소감(reflection)도 함께 삭제됩니다.
     *
     * @throws IllegalStateException 응답 상태가 아닌 경우
     */
    fun cancelAnswer() {
        if (status != PrayerTopicStatus.ANSWERED) {
            throw PrayerTopicNotAnsweredException(id.value)
        }
        this.status = PrayerTopicStatus.PRAYING
        this.answeredAt = null
        this.reflection = null
        touch()
    }

    /**
     * 응답된 기도제목의 소감을 수정합니다.
     * reflection은 암호문(Base64)입니다.
     *
     * @param newReflection 새 소감 (null로 설정 시 소감 삭제)
     * @throws IllegalStateException 응답 상태가 아닌 경우
     */
    fun updateReflection(newReflection: String?) {
        if (status != PrayerTopicStatus.ANSWERED) {
            throw PrayerTopicNotAnsweredException(id.value)
        }
        if (reflection != newReflection) {
            this.reflection = newReflection
            touch()
        }
    }

    companion object {
        /**
         * 새로운 기도제목을 생성합니다.
         *
         * @param memberId 소유자 ID
         * @param title 기도제목 (암호문)
         */
        fun create(
            memberId: MemberId,
            title: String,
        ): PrayerTopic {
            require(title.isNotBlank()) { "Title cannot be blank" }

            val now = LocalDateTime.now()
            return PrayerTopic(
                id = PrayerTopicId.new(),
                memberId = memberId,
                title = title,
                status = PrayerTopicStatus.PRAYING,
                answeredAt = null,
                reflection = null,
                version = null,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
