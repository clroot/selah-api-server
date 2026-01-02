package io.clroot.selah.domains.member.adapter.outbound.persistence.session

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/**
 * Session JPA Repository
 */
@Repository
interface SessionJpaRepository : JpaRepository<SessionEntity, String> {
    /**
     * 회원의 모든 세션을 삭제합니다.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SessionEntity s WHERE s.memberId = :memberId")
    fun deleteAllByMemberId(
        @Param("memberId") memberId: String,
    ): Int

    /**
     * 만료된 세션을 삭제합니다.
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM SessionEntity s WHERE s.expiresAt < :now")
    fun deleteExpiredSessions(
        @Param("now") now: LocalDateTime,
    ): Int
}
