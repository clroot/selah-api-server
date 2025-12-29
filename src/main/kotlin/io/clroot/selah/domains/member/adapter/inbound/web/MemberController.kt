package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.util.HttpRequestUtils
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.member.adapter.inbound.web.dto.*
import io.clroot.selah.domains.member.application.port.inbound.GetCurrentMemberUseCase
import io.clroot.selah.domains.member.application.port.inbound.ManageApiKeyUseCase
import io.clroot.selah.domains.member.application.port.inbound.UpdateProfileCommand
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 회원 관련 Controller
 */
@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val getCurrentMemberUseCase: GetCurrentMemberUseCase,
    private val manageApiKeyUseCase: ManageApiKeyUseCase,
) {

    /**
     * 현재 로그인한 회원 정보 조회
     */
    @GetMapping("/me")
    suspend fun getMyProfile(): ResponseEntity<ApiResponse<MemberProfileResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val member = getCurrentMemberUseCase.getMember(memberId)

        return ResponseEntity.ok(ApiResponse.success(member.toProfileResponse()))
    }

    /**
     * 프로필 업데이트
     */
    @PatchMapping("/me")
    suspend fun updateMyProfile(
        @RequestBody request: UpdateProfileRequest,
    ): ResponseEntity<ApiResponse<MemberProfileResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val member = getCurrentMemberUseCase.updateProfile(
            memberId,
            UpdateProfileCommand(
                nickname = request.nickname,
                profileImageUrl = request.profileImageUrl,
            )
        )

        return ResponseEntity.ok(ApiResponse.success(member.toProfileResponse()))
    }

    // === API Key 관리 ===

    /**
     * API Key 목록 조회
     */
    @GetMapping("/me/api-keys")
    suspend fun listApiKeys(): ResponseEntity<ApiResponse<List<ApiKeyResponse>>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val apiKeys = manageApiKeyUseCase.listApiKeys(memberId)

        return ResponseEntity.ok(ApiResponse.success(apiKeys.map { it.toResponse() }))
    }

    /**
     * API Key 생성
     */
    @PostMapping("/me/api-keys")
    suspend fun createApiKey(
        @RequestBody request: CreateApiKeyRequest,
        httpRequest: HttpServletRequest,
    ): ResponseEntity<ApiResponse<ApiKeyCreateResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val result = manageApiKeyUseCase.createApiKey(memberId, request.name, HttpRequestUtils.extractIpAddress(httpRequest))

        return ResponseEntity.ok(ApiResponse.success(result.toResponse()))
    }

    /**
     * API Key 삭제
     */
    @DeleteMapping("/me/api-keys/{apiKeyId}")
    suspend fun deleteApiKey(
        @PathVariable apiKeyId: String,
    ): ResponseEntity<ApiResponse<Unit>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        manageApiKeyUseCase.deleteApiKey(memberId, apiKeyId)

        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
