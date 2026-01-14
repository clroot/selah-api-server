package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Prayer Entity Repository
 *
 * 기본 CRUD 작업과 단순 쿼리를 위한 CoroutineCrudRepository 인터페이스입니다.
 * 복잡한 쿼리(서브쿼리, GROUP BY 등)는 JDSL을 통해 PersistenceAdapter에서 처리합니다.
 */
interface PrayerEntityRepository : CoroutineCrudRepository<PrayerEntity, String> {
    suspend fun findAllByMemberId(memberId: String, pageable: Pageable): Page<PrayerEntity>

    suspend fun findByIdAndMemberId(id: String, memberId: String): PrayerEntity?
}
