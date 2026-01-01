package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.IdClass
import jakarta.persistence.Table
import java.io.Serializable

data class PrayerPrayerTopicId(
    val prayerId: String = "",
    val prayerTopicId: String = "",
) : Serializable

@Entity
@Table(name = "prayer_prayer_topics")
@IdClass(PrayerPrayerTopicId::class)
class PrayerPrayerTopicEntity(
    @Id
    @Column(name = "prayer_id", length = 26)
    val prayerId: String,
    @Id
    @Column(name = "prayer_topic_id", length = 26)
    val prayerTopicId: String,
)
