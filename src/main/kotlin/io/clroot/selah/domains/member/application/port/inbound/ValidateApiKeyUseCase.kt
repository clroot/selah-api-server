package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.application.port.outbound.ApiKeyInfo

/**
 * API Key 검증 UseCase
 */
interface ValidateApiKeyUseCase {
    /**
     * API Key를 검증합니다.
     *
     * @param apiKey API Key 원본
     * @param ipAddress 클라이언트 IP 주소
     * @return API Key 정보 또는 null (유효하지 않은 경우)
     */
    suspend fun validate(
        apiKey: String,
        ipAddress: String?,
    ): ApiKeyInfo?
}
