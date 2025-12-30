package io.clroot.selah.domains.prayer.adapter.inbound.web.dto

import io.clroot.selah.domains.prayer.domain.Prayer
import java.time.LocalDateTime

// === Request DTOs ===

data class CreatePrayerRequest(
    val content: String,
)

data class UpdatePrayerRequest(
    val content: String,
)

// === Response DTOs ===

data class PrayerResponse(
    val id: String,
    val content: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
)

// === Extension Functions ===

fun Prayer.toResponse(): PrayerResponse = PrayerResponse(
    id = id.value,
    content = content,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
