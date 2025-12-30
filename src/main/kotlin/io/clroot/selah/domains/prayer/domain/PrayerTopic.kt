package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.common.domain.AggregateRoot
import io.clroot.selah.domains.member.domain.MemberId
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

    companion object {
        /**
         * 새로운 기도제목을 생성합니다.
         *
         * @param memberId 소유자 ID
         * @param title 기도제목 (암호문)
         */
        fun create(memberId: MemberId, title: String): PrayerTopic {
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
