package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Prayer Entity Repository
 *
 * 기본 CRUD 작업을 위한 CoroutineCrudRepository 인터페이스입니다.
 *
 * Note: 커스텀 쿼리 메서드(findByXxx)는 Spring Data 4.x 호환성 문제로 지원하지 않습니다.
 * 복잡한 쿼리는 JDSL을 통해 PersistenceAdapter에서 직접 처리합니다.
 */
interface PrayerEntityRepository : CoroutineCrudRepository<PrayerEntity, String>
