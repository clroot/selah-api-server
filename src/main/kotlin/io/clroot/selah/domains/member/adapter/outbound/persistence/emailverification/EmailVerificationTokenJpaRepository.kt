package io.clroot.selah.domains.member.adapter.outbound.persistence.emailverification

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import java.time.LocalDateTime

interface EmailVerificationTokenJpaRepository : JpaRepository<EmailVerificationTokenEntity, String> {

    fun findByTokenHash(tokenHash: String): EmailVerificationTokenEntity?

    fun findTopByMemberIdOrderByCreatedAtDesc(memberId: String): EmailVerificationTokenEntity?

    @Modifying
    @Query("DELETE FROM EmailVerificationTokenEntity e WHERE e.memberId = :memberId")
    fun deleteAllByMemberId(memberId: String)

    @Modifying
    @Query("DELETE FROM EmailVerificationTokenEntity e WHERE e.expiresAt < :now")
    fun deleteAllByExpiresAtBefore(now: LocalDateTime): Int
}
