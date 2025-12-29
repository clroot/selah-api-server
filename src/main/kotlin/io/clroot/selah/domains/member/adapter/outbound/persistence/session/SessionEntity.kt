package io.clroot.selah.domains.member.adapter.outbound.persistence.session

import io.clroot.selah.domains.member.domain.Member
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Session JPA Entity
 *
 * 세션 정보를 DB에 저장합니다.
 * 추후 Redis로 전환 시 이 Entity와 Repository를 교체합니다.
 */
@Entity
@Table(
    name = "sessions",
    indexes = [
        Index(name = "idx_sessions_member_id", columnList = "member_id"),
        Index(name = "idx_sessions_expires_at", columnList = "expires_at"),
    ],
)
class SessionEntity(
    /**
     * 세션 토큰 (UUID 기반)
     */
    @Id
    @Column(name = "token", length = 36)
    val token: String,

    /**
     * 회원 ID (ULID)
     */
    @Column(name = "member_id", nullable = false, length = 26)
    val memberId: String,

    /**
     * 회원 역할 (세션에 캐시)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    val role: Member.Role,

    /**
     * 클라이언트 User-Agent
     */
    @Column(name = "user_agent", length = 500)
    val userAgent: String?,

    /**
     * 세션 생성 시 IP 주소
     */
    @Column(name = "created_ip", length = 45)
    val createdIp: String?,

    /**
     * 마지막 접근 IP 주소
     */
    @Column(name = "last_accessed_ip", length = 45)
    var lastAccessedIp: String?,

    /**
     * 세션 만료 시간
     */
    @Column(name = "expires_at", nullable = false)
    var expiresAt: LocalDateTime,

    /**
     * 세션 생성 시간
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,
)
