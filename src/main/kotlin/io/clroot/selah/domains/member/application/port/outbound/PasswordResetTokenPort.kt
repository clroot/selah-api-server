package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.MemberId
import java.time.LocalDateTime

/**
 * 비밀번호 재설정 토큰 관리를 위한 Outbound Port
 *
 * 토큰은 SHA-256 해시로 저장되며, 원본 토큰은 생성 시 1회만 반환됩니다.
 */
interface PasswordResetTokenPort {
    /**
     * 새 비밀번호 재설정 토큰을 생성합니다.
     * 기존 토큰이 있으면 무효화됩니다.
     *
     * @param memberId 회원 ID
     * @return 생성된 토큰 정보 (원본 토큰 포함)
     */
    suspend fun create(memberId: MemberId): PasswordResetTokenCreateResult

    /**
     * 토큰으로 유효한 재설정 정보를 조회합니다.
     * 만료되거나 사용된 토큰은 반환되지 않습니다.
     *
     * @param token 원본 토큰
     * @return 토큰 정보 또는 null
     */
    suspend fun findValidByToken(token: String): PasswordResetTokenInfo?

    /**
     * 토큰을 사용 처리합니다.
     *
     * @param id 토큰 ID
     */
    suspend fun markAsUsed(id: String)

    /**
     * 특정 회원의 모든 토큰을 무효화합니다.
     *
     * @param memberId 회원 ID
     */
    suspend fun invalidateAllByMemberId(memberId: MemberId)

    /**
     * 회원의 최근 토큰 생성 시간을 조회합니다. (재발송 제한용)
     *
     * @param memberId 회원 ID
     * @return 최근 토큰 생성 시간 또는 null
     */
    suspend fun findLatestCreatedAtByMemberId(memberId: MemberId): LocalDateTime?

    /**
     * 만료된 토큰을 정리합니다.
     * 스케줄러에서 주기적으로 호출합니다.
     *
     * @return 삭제된 토큰 수
     */
    suspend fun deleteExpiredTokens(): Int
}

/**
 * 비밀번호 재설정 토큰 생성 결과
 *
 * @param info 토큰 정보
 * @param rawToken 원본 토큰 (최초 1회만 반환, 저장되지 않음)
 */
data class PasswordResetTokenCreateResult(
    val info: PasswordResetTokenInfo,
    val rawToken: String,
)

/**
 * 비밀번호 재설정 토큰 정보
 */
data class PasswordResetTokenInfo(
    val id: String,
    val memberId: MemberId,
    val expiresAt: LocalDateTime,
    val usedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
) {
    fun isExpired(): Boolean = LocalDateTime.now().isAfter(expiresAt)

    fun isUsed(): Boolean = usedAt != null

    fun isValid(): Boolean = !isExpired() && !isUsed()
}
