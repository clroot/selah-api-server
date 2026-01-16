package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

/**
 * PrayerTopic JPA Entity
 *
 * Domain의 PrayerTopic과 분리된 Persistence Layer Entity입니다.
 * JPA 어노테이션은 Adapter Layer에서만 사용합니다.
 */
@Entity
@Table(
    name = "prayer_topics",
    indexes = [
        Index(name = "idx_prayer_topics_member_id", columnList = "member_id"),
        Index(name = "idx_prayer_topics_member_status", columnList = "member_id, status"),
        Index(name = "idx_prayer_topics_member_created", columnList = "member_id, created_at DESC"),
    ],
)
class PrayerTopicEntity(
    @Id
    @Column(name = "id", length = 26)
    val id: String,
    @Column(name = "member_id", length = 26, nullable = false)
    val memberId: String,
    @Column(name = "title", nullable = false, columnDefinition = "TEXT")
    var title: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: PrayerTopicStatus,
    @Column(name = "answered_at")
    var answeredAt: LocalDateTime?,
    @Column(name = "reflection", columnDefinition = "TEXT")
    var reflection: String?,
    @Version
    @Column(name = "version")
    var version: Long?,
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)
