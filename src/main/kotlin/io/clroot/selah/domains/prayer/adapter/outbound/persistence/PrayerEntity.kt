package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import jakarta.persistence.CollectionTable
import jakarta.persistence.Column
import jakarta.persistence.ElementCollection
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.Table
import jakarta.persistence.Version
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import java.time.LocalDateTime

/**
 * Prayer JPA Entity
 *
 * Domain의 Prayer와 분리된 Persistence Layer Entity입니다.
 * JPA 어노테이션은 Adapter Layer에서만 사용합니다.
 */
@Entity
@Table(
    name = "prayers",
    indexes = [
        Index(name = "idx_prayers_member_id", columnList = "member_id"),
        Index(name = "idx_prayers_member_created", columnList = "member_id, created_at DESC"),
    ],
)
class PrayerEntity(
    @Id
    @Column(name = "id", length = 26)
    val id: String,
    @Column(name = "member_id", length = 26, nullable = false)
    val memberId: String,
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
        name = "prayer_prayer_topics",
        joinColumns = [JoinColumn(name = "prayer_id")],
        indexes = [
            Index(name = "idx_prayer_prayer_topics_prayer_id", columnList = "prayer_id"),
            Index(name = "idx_prayer_prayer_topics_prayer_topic_id", columnList = "prayer_topic_id"),
        ],
    )
    @Column(name = "prayer_topic_id", length = 26)
    val prayerTopicIds: MutableList<String> = mutableListOf(),
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,
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
