package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import org.springframework.stereotype.Component

/**
 * PrayerTopic Domain-Entity 매퍼
 */
@Component
class PrayerTopicMapper {

    /**
     * Domain → Entity 변환
     */
    fun toEntity(prayerTopic: PrayerTopic): PrayerTopicEntity {
        return PrayerTopicEntity(
            id = prayerTopic.id.value,
            memberId = prayerTopic.memberId.value,
            title = prayerTopic.title,
            status = prayerTopic.status,
            answeredAt = prayerTopic.answeredAt,
            reflection = prayerTopic.reflection,
            version = prayerTopic.version,
            createdAt = prayerTopic.createdAt,
            updatedAt = prayerTopic.updatedAt,
        )
    }

    /**
     * Entity → Domain 변환
     */
    fun toDomain(entity: PrayerTopicEntity): PrayerTopic {
        return PrayerTopic(
            id = PrayerTopicId.from(entity.id),
            memberId = MemberId.from(entity.memberId),
            title = entity.title,
            status = entity.status,
            answeredAt = entity.answeredAt,
            reflection = entity.reflection,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    /**
     * 기존 Entity를 Domain 데이터로 업데이트
     */
    fun updateEntity(entity: PrayerTopicEntity, prayerTopic: PrayerTopic) {
        entity.title = prayerTopic.title
        entity.status = prayerTopic.status
        entity.answeredAt = prayerTopic.answeredAt
        entity.reflection = prayerTopic.reflection
        entity.updatedAt = prayerTopic.updatedAt
    }
}
