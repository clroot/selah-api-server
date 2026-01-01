package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import java.time.LocalDateTime

/**
 * 세션 관리를 위한 Outbound Port
 *
 * DB 기반 구현과 Redis 기반 구현을 교체 가능하도록 추상화합니다.
 */
interface SessionPort {
    /**
     * 새 세션을 생성합니다.
     *
     * @param memberId 회원 ID
     * @param role 회원 역할 (세션에 캐시)
     * @param userAgent 클라이언트 User-Agent
     * @param ipAddress 클라이언트 IP 주소
     * @return 생성된 세션 정보
     */
    suspend fun create(
        memberId: MemberId,
        role: Member.Role,
        userAgent: String?,
        ipAddress: String?,
    ): SessionInfo

    /**
     * 세션 토큰으로 세션을 조회합니다.
     *
     * @param token 세션 토큰
     * @return 세션 정보 또는 null
     */
    suspend fun findByToken(token: String): SessionInfo?

    /**
     * 세션을 삭제합니다. (로그아웃)
     *
     * @param token 세션 토큰
     */
    suspend fun delete(token: String)

    /**
     * 특정 회원의 모든 세션을 삭제합니다. (전체 로그아웃)
     *
     * @param memberId 회원 ID
     */
    suspend fun deleteAllByMemberId(memberId: MemberId)

    /**
     * 세션 만료 시간을 연장하고, 마지막 접근 IP를 업데이트합니다. (Sliding Session)
     *
     * @param token 세션 토큰
     * @param ipAddress 클라이언트 IP 주소
     */
    suspend fun extendExpiry(
        token: String,
        ipAddress: String?,
    )

    /**
     * 만료된 세션을 정리합니다.
     * 스케줄러에서 주기적으로 호출합니다.
     *
     * @return 삭제된 세션 수
     */
    suspend fun deleteExpiredSessions(): Int
}

/**
 * 세션 정보 데이터 클래스
 */
data class SessionInfo(
    val token: String,
    val memberId: MemberId,
    val role: Member.Role,
    val userAgent: String?,
    val createdIp: String?,
    val lastAccessedIp: String?,
    val expiresAt: LocalDateTime,
    val createdAt: LocalDateTime,
) {
    /**
     * 세션이 만료되었는지 확인합니다.
     */
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)
}
