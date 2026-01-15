package io.clroot.selah.domains.member.adapter.outbound.persistence.session

import org.springframework.data.repository.kotlin.CoroutineCrudRepository

/**
 * Session Entity Repository
 *
 * 기본 CRUD 작업과 단순 쿼리를 위한 CoroutineCrudRepository 인터페이스입니다.
 */
interface SessionEntityRepository : CoroutineCrudRepository<SessionEntity, String> {
    suspend fun findByToken(token: String): SessionEntity?

    suspend fun deleteByToken(token: String)

    suspend fun deleteAllByMemberId(memberId: String)
}
