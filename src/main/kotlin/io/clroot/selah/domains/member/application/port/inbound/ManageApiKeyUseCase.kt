package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.application.port.outbound.ApiKeyCreateResult
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyInfo
import io.clroot.selah.domains.member.domain.MemberId

/**
 * API Key 관리 UseCase
 */
interface ManageApiKeyUseCase {
    /**
     * 새 API Key를 생성합니다.
     *
     * @param memberId 회원 ID
     * @param name API Key 이름
     * @param ipAddress 생성 시 IP 주소
     * @return 생성된 API Key (원본 키 포함, 최초 1회만)
     */
    suspend fun createApiKey(
        memberId: MemberId,
        name: String,
        ipAddress: String?,
    ): ApiKeyCreateResult

    /**
     * 회원의 모든 API Key 목록을 조회합니다.
     *
     * @param memberId 회원 ID
     * @return API Key 목록 (원본 키 미포함)
     */
    suspend fun listApiKeys(memberId: MemberId): List<ApiKeyInfo>

    /**
     * API Key를 삭제합니다.
     *
     * @param memberId 회원 ID (권한 검증용)
     * @param apiKeyId API Key ID
     */
    suspend fun deleteApiKey(
        memberId: MemberId,
        apiKeyId: String,
    )
}
