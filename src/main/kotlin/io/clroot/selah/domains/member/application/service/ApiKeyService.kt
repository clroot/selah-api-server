package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.ManageApiKeyUseCase
import io.clroot.selah.domains.member.application.port.inbound.ValidateApiKeyUseCase
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyCreateResult
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyInfo
import io.clroot.selah.domains.member.application.port.outbound.ApiKeyPort
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * API Key 관리 서비스
 */
@Service
@Transactional
class ApiKeyService(
    private val apiKeyPort: ApiKeyPort,
    private val loadMemberPort: LoadMemberPort,
) : ValidateApiKeyUseCase,
    ManageApiKeyUseCase {
    @Transactional(readOnly = true)
    override suspend fun validate(
        apiKey: String,
        ipAddress: String?,
    ): ApiKeyInfo? {
        val info = apiKeyPort.findByKey(apiKey) ?: return null

        // 마지막 사용 시간 및 IP 업데이트
        apiKeyPort.updateLastUsedAt(info.id, ipAddress)

        return info
    }

    override suspend fun createApiKey(
        memberId: MemberId,
        name: String,
        ipAddress: String?,
    ): ApiKeyCreateResult {
        // 회원 존재 확인 및 역할 조회
        val member =
            loadMemberPort.findById(memberId)
                ?: throw MemberNotFoundException(memberId.value)

        return apiKeyPort.create(
            memberId = memberId,
            role = member.role,
            name = name,
            ipAddress = ipAddress,
        )
    }

    @Transactional(readOnly = true)
    override suspend fun listApiKeys(memberId: MemberId): List<ApiKeyInfo> = apiKeyPort.findAllByMemberId(memberId)

    override suspend fun deleteApiKey(
        memberId: MemberId,
        apiKeyId: String,
    ) {
        // 권한 검증: API Key 소유자인지 확인
        val apiKeys = apiKeyPort.findAllByMemberId(memberId)
        val isOwner = apiKeys.any { it.id == apiKeyId }

        if (isOwner) {
            apiKeyPort.delete(apiKeyId)
        }
        // 소유자가 아니면 무시 (보안상 에러 노출 안함)
    }
}
