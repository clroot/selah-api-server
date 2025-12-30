package io.clroot.selah.domains.member.adapter.outbound.persistence.passwordreset

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

interface PasswordResetTokenJpaRepository : JpaRepository<PasswordResetTokenEntity, String> {

    fun findByTokenHash(tokenHash: String): PasswordResetTokenEntity?

    fun findTopByMemberIdOrderByCreatedAtDesc(memberId: String): PasswordResetTokenEntity?

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetTokenEntity e WHERE e.memberId = :memberId")
    fun deleteAllByMemberId(memberId: String)

    @Modifying
    @Transactional
    @Query("DELETE FROM PasswordResetTokenEntity e WHERE e.expiresAt < :now")
    fun deleteAllByExpiresAtBefore(now: LocalDateTime): Int
}
