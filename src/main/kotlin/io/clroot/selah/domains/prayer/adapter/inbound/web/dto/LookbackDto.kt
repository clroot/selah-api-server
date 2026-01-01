package io.clroot.selah.domains.prayer.adapter.inbound.web.dto

import io.clroot.selah.domains.prayer.application.port.inbound.LookbackResult
import java.time.LocalDate

data class LookbackResponse(
    val prayerTopic: PrayerTopicResponse,
    val selectedAt: LocalDate,
    val daysSinceCreated: Long,
)

fun LookbackResult.toResponse(): LookbackResponse =
    LookbackResponse(
        prayerTopic = prayerTopic.toResponse(),
        selectedAt = selectedAt,
        daysSinceCreated = daysSinceCreated,
    )
