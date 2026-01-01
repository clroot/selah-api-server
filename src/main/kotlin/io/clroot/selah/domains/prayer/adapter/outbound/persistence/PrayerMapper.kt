package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.Prayer
import io.clroot.selah.domains.prayer.domain.PrayerId
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import org.springframework.stereotype.Component

/**
 * Prayer Domain-Entity 매퍼
 */
@Component
class PrayerMapper {
    /**
     * Domain → Entity 변환
     */
    fun toEntity(prayer: Prayer): PrayerEntity =
        PrayerEntity(
            id = prayer.id.value,
            memberId = prayer.memberId.value,
            prayerTopicIds = prayer.prayerTopicIds.map { it.value }.toMutableList(),
            content = prayer.content,
            version = prayer.version,
            createdAt = prayer.createdAt,
            updatedAt = prayer.updatedAt,
        )

    /**
     * Entity → Domain 변환
     */
    fun toDomain(entity: PrayerEntity): Prayer =
        Prayer(
            id = PrayerId.from(entity.id),
            memberId = MemberId.from(entity.memberId),
            prayerTopicIds = entity.prayerTopicIds.map { PrayerTopicId.from(it) },
            content = entity.content,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )

    /**
     * 기존 Entity를 Domain 데이터로 업데이트
     */
    fun updateEntity(
        entity: PrayerEntity,
        prayer: Prayer,
    ) {
        entity.content = prayer.content
        entity.prayerTopicIds.clear()
        entity.prayerTopicIds.addAll(prayer.prayerTopicIds.map { it.value })
        entity.updatedAt = prayer.updatedAt
    }
}
