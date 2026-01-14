package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * PrayerTopic Entity Repository
 *
 * 기본 CRUD 작업과 단순 쿼리를 위한 CoroutineCrudRepository 인터페이스입니다.
 * 복잡한 쿼리(동적 조건, 페이지네이션 등)는 JDSL을 통해 PersistenceAdapter에서 처리합니다.
 */
interface PrayerTopicEntityRepository : CoroutineCrudRepository<PrayerTopicEntity, String> {
    suspend fun findByIdAndMemberId(id: String, memberId: String): PrayerTopicEntity?
}
