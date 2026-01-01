package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.common.domain.AggregateRoot
import io.clroot.selah.domains.member.domain.MemberId
import java.time.LocalDateTime

/**
 * Prayer Aggregate Root
 *
 * 기도문을 관리합니다.
 * content는 E2E 암호화된 암호문(Base64)으로 저장됩니다.
 * prayerTopicIds는 연결된 기도제목 ID 목록입니다 (여러 개 가능).
 */
class Prayer(
    override val id: PrayerId,
    // --- 비즈니스 필드 ---
    val memberId: MemberId,
    prayerTopicIds: List<PrayerTopicId>,
    content: String,
    // --- 메타 필드 (하단) ---
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<PrayerId>(id, version, createdAt, updatedAt) {

    init {
        require(content.isNotBlank()) { "Content cannot be blank" }
    }

    private val _prayerTopicIds: MutableList<PrayerTopicId> = prayerTopicIds.toMutableList()
    val prayerTopicIds: List<PrayerTopicId> get() = _prayerTopicIds.toList()

    var content: String = content
        private set

    /**
     * 기도문(content)을 수정합니다.
     * content는 암호문(Base64)입니다.
     */
    fun updateContent(newContent: String) {
        require(newContent.isNotBlank()) { "Content cannot be blank" }
        if (content != newContent) {
            content = newContent
            touch()
        }
    }

    companion object {
        /**
         * 새로운 기도문을 생성합니다.
         *
         * @param memberId 작성자 ID
         * @param prayerTopicIds 연결된 기도제목 ID 목록
         * @param content 기도문 내용 (암호문)
         */
        fun create(
            memberId: MemberId,
            prayerTopicIds: List<PrayerTopicId>,
            content: String,
        ): Prayer {
            require(content.isNotBlank()) { "Content cannot be blank" }

            val now = LocalDateTime.now()
            return Prayer(
                id = PrayerId.new(),
                memberId = memberId,
                prayerTopicIds = prayerTopicIds,
                content = content,
                version = null,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
