package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.LocalDate
import java.time.LocalDateTime

@Entity
@Table(
    name = "lookback_selections",
    indexes = [
        Index(
            name = "idx_lookback_selections_member_date",
            columnList = "member_id, selected_at",
            unique = true,
        ),
        Index(
            name = "idx_lookback_selections_member_recent",
            columnList = "member_id, selected_at DESC",
        ),
    ],
)
class LookbackSelectionEntity(
    @Id
    @Column(name = "id", length = 26)
    val id: String,
    @Column(name = "member_id", length = 26, nullable = false)
    val memberId: String,
    @Column(name = "prayer_topic_id", length = 26, nullable = false)
    val prayerTopicId: String,
    @Column(name = "selected_at", nullable = false)
    val selectedAt: LocalDate,
    @Version
    @Column(name = "version")
    var version: Long?,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
)
