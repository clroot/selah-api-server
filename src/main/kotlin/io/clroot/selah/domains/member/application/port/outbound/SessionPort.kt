package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import java.time.LocalDateTime

/**
 * 세션 관리를 위한 Outbound Port
 *
 * DB 기반 구현과 Redis 기반 구현을 교체 가능하도록 추상화합니다.
 * 순수 데이터 접근만 담당하며, 비즈니스 로직은 Application Layer에서 처리합니다.
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
     * 세션 정보를 업데이트합니다.
     *
     * @param sessionInfo 업데이트할 세션 정보
     * @return 업데이트된 세션 정보
     */
    suspend fun update(sessionInfo: SessionInfo): SessionInfo

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
     * 만료된 세션을 삭제합니다.
     *
     * @param before 이 시간 이전에 만료된 세션을 삭제
     * @return 삭제된 세션 수
     */
    suspend fun deleteExpiredBefore(before: LocalDateTime): Long
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
