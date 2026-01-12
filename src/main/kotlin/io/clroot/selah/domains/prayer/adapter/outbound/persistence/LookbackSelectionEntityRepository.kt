package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * LookbackSelection Entity Repository
 *
 * 단순 CRUD 작업을 위한 CoroutineCrudRepository 인터페이스입니다.
 * 복잡한 쿼리(날짜 범위, 동적 조건 등)는 JDSL을 통해 PersistenceAdapter에서 직접 처리합니다.
 */
interface LookbackSelectionEntityRepository : CoroutineCrudRepository<LookbackSelectionEntity, String>
