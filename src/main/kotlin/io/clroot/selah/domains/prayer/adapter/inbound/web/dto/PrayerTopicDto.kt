package io.clroot.selah.domains.prayer.adapter.inbound.web.dto

import io.clroot.selah.domains.prayer.domain.PrayerTopic
import io.clroot.selah.domains.prayer.domain.PrayerTopicStatus
import java.time.LocalDateTime

// === Request DTOs ===

data class CreatePrayerTopicRequest(
    val title: String,
)

data class UpdatePrayerTopicRequest(
    val title: String,
)

// === Response DTOs ===

data class PrayerTopicResponse(
    val id: String,
    val title: String,
    val status: PrayerTopicStatus,
    val answeredAt: LocalDateTime?,
    val reflection: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

// === Extension Functions ===

fun PrayerTopic.toResponse(): PrayerTopicResponse = PrayerTopicResponse(
    id = id.value,
    title = title,
    status = status,
    answeredAt = answeredAt,
    reflection = reflection,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
