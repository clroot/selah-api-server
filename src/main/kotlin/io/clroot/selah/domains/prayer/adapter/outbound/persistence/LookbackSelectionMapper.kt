package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.prayer.domain.LookbackSelection
import io.clroot.selah.domains.prayer.domain.LookbackSelectionId
import io.clroot.selah.domains.prayer.domain.PrayerTopicId
import org.springframework.stereotype.Component

@Component
class LookbackSelectionMapper {
    fun toEntity(selection: LookbackSelection): LookbackSelectionEntity =
        LookbackSelectionEntity(
            id = selection.id.value,
            memberId = selection.memberId.value,
            prayerTopicId = selection.prayerTopicId.value,
            selectedAt = selection.selectedAt,
            version = selection.version,
            createdAt = selection.createdAt,
            updatedAt = selection.updatedAt,
        )

    fun toDomain(entity: LookbackSelectionEntity): LookbackSelection =
        LookbackSelection(
            id = LookbackSelectionId.from(entity.id),
            memberId = MemberId.from(entity.memberId),
            prayerTopicId = PrayerTopicId.from(entity.prayerTopicId),
            selectedAt = entity.selectedAt,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
}
