package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
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

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    var content: String,

    @Version
    @Column(name = "version")
    var version: Long?,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)
