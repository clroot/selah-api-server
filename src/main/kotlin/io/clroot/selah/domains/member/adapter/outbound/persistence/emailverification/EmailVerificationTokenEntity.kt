package io.clroot.selah.domains.member.adapter.outbound.persistence.emailverification

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import java.time.LocalDateTime

/**
 * 이메일 인증 토큰 JPA Entity
 *
 * 보안을 위해 원본 토큰은 저장하지 않고 해시만 저장합니다.
 */
@Entity
@Table(
    name = "email_verification_tokens",
    indexes = [
        Index(name = "idx_email_verification_tokens_member_id", columnList = "member_id"),
        Index(name = "idx_email_verification_tokens_token_hash", columnList = "token_hash", unique = true),
        Index(name = "idx_email_verification_tokens_expires_at", columnList = "expires_at"),
    ],
)
class EmailVerificationTokenEntity(
    /**
     * 토큰 ID (ULID)
     */
    @Id
    @Column(name = "id", length = 26)
    val id: String,
    /**
     * 토큰 해시 (SHA-256, 원본 저장 안함)
     */
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    val tokenHash: String,
    /**
     * 회원 ID
     */
    @Column(name = "member_id", nullable = false, length = 26)
    val memberId: String,
    /**
     * 만료 시간
     */
    @Column(name = "expires_at", nullable = false)
    val expiresAt: LocalDateTime,
    /**
     * 사용 시간 (null이면 미사용)
     */
    @Column(name = "used_at")
    var usedAt: LocalDateTime? = null,
    /**
     * 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,
)
