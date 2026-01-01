package io.clroot.selah.domains.prayer.domain

import io.clroot.selah.common.domain.AggregateRoot
import io.clroot.selah.domains.member.domain.MemberId
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * LookbackSelection Aggregate Root
 *
 * 돌아보기 선정 기록을 관리합니다.
 * 하루에 한 번 기도제목을 랜덤 선정하여 과거 기도를 돌아볼 수 있게 합니다.
 */
class LookbackSelection(
    override val id: LookbackSelectionId,
    // --- 비즈니스 필드 ---
    val memberId: MemberId,
    val prayerTopicId: PrayerTopicId,
    val selectedAt: LocalDate,
    // --- 메타 필드 (하단) ---
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<LookbackSelectionId>(id, version, createdAt, updatedAt) {
    companion object {
        /**
         * 새로운 돌아보기 선정을 생성합니다.
         *
         * @param memberId 회원 ID
         * @param prayerTopicId 선정된 기도제목 ID
         */
        fun create(
            memberId: MemberId,
            prayerTopicId: PrayerTopicId,
        ): LookbackSelection {
            val now = LocalDateTime.now()
            return LookbackSelection(
                id = LookbackSelectionId.new(),
                memberId = memberId,
                prayerTopicId = prayerTopicId,
                selectedAt = now.toLocalDate(),
                version = null,
                createdAt = now,
                updatedAt = now,
            )
        }
    }
}
