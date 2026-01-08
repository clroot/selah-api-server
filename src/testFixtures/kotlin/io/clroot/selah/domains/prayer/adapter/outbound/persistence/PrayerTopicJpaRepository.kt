package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface PrayerTopicJpaRepository : JpaRepository<PrayerTopicEntity, String>
