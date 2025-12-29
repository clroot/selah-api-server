package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import java.time.LocalDateTime

/**
 * API Key 관리를 위한 Outbound Port
 *
 * 외부 연동, CLI 도구 등에서 사용하는 API Key를 관리합니다.
 */
interface ApiKeyPort {
    /**
     * 새 API Key를 생성합니다.
     *
     * @param memberId 회원 ID
     * @param role 회원 역할
     * @param name API Key 이름 (구분용)
     * @param ipAddress 생성 시 IP 주소
     * @return 생성된 API Key 정보 (원본 키 포함, 최초 1회만 반환)
     */
    suspend fun create(
        memberId: MemberId,
        role: Member.Role,
        name: String,
        ipAddress: String?,
    ): ApiKeyCreateResult

    /**
     * API Key로 정보를 조회합니다.
     *
     * @param apiKey API Key 원본
     * @return API Key 정보 또는 null
     */
    suspend fun findByKey(apiKey: String): ApiKeyInfo?

    /**
     * API Key를 삭제합니다.
     *
     * @param id API Key ID
     */
    suspend fun delete(id: String)

    /**
     * 회원의 모든 API Key 목록을 조회합니다.
     *
     * @param memberId 회원 ID
     * @return API Key 목록
     */
    suspend fun findAllByMemberId(memberId: MemberId): List<ApiKeyInfo>

    /**
     * API Key 마지막 사용 시간과 IP를 업데이트합니다.
     *
     * @param id API Key ID
     * @param ipAddress 마지막 사용 IP 주소
     */
    suspend fun updateLastUsedAt(id: String, ipAddress: String?)
}

/**
 * API Key 생성 결과
 *
 * @param info API Key 정보
 * @param rawKey 원본 API Key (최초 1회만 반환, 저장되지 않음)
 */
data class ApiKeyCreateResult(
    val info: ApiKeyInfo,
    val rawKey: String,
)

/**
 * API Key 정보 데이터 클래스
 */
data class ApiKeyInfo(
    val id: String,
    val memberId: MemberId,
    val role: Member.Role,
    val name: String,
    val prefix: String,
    val createdIp: String?,
    val lastUsedIp: String?,
    val createdAt: LocalDateTime,
    val lastUsedAt: LocalDateTime?,
)
