package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.common.util.HttpRequestUtils
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.member.adapter.inbound.web.dto.*
import io.clroot.selah.domains.member.application.port.inbound.ChangePasswordCommand
import io.clroot.selah.domains.member.application.port.inbound.ChangePasswordUseCase
import io.clroot.selah.domains.member.application.port.inbound.ConnectOAuthCommand
import io.clroot.selah.domains.member.application.port.inbound.GetCurrentMemberUseCase
import io.clroot.selah.domains.member.application.port.inbound.ManageApiKeyUseCase
import io.clroot.selah.domains.member.application.port.inbound.ManageOAuthConnectionUseCase
import io.clroot.selah.domains.member.application.port.inbound.SetPasswordCommand
import io.clroot.selah.domains.member.application.port.inbound.UpdateProfileCommand
import io.clroot.selah.domains.member.domain.OAuthProvider
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * 회원 관련 Controller
 */
@RestController
@RequestMapping("/api/v1/members")
class MemberController(
    private val getCurrentMemberUseCase: GetCurrentMemberUseCase,
    private val changePasswordUseCase: ChangePasswordUseCase,
    private val manageApiKeyUseCase: ManageApiKeyUseCase,
    private val manageOAuthConnectionUseCase: ManageOAuthConnectionUseCase,
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

    // === 비밀번호 관리 ===

    @PutMapping("/me/password")
    suspend fun changePassword(
        @RequestBody request: ChangePasswordRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        changePasswordUseCase.changePassword(
            memberId,
            ChangePasswordCommand(
                currentPassword = request.currentPassword,
                newPassword = request.newPassword,
            ),
        )

        return ResponseEntity.ok(ApiResponse.success(Unit))
    }

    @PostMapping("/me/password")
    suspend fun setPassword(
        @RequestBody request: SetPasswordRequest,
    ): ResponseEntity<ApiResponse<Unit>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        changePasswordUseCase.setPassword(
            memberId,
            SetPasswordCommand(newPassword = request.newPassword),
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(Unit))
    }

    // === API Key 관리 ===

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

    // === OAuth 연결 관리 ===

    /**
     * OAuth 연결 목록 조회
     */
    @GetMapping("/me/oauth-connections")
    suspend fun getOAuthConnections(): ResponseEntity<ApiResponse<OAuthConnectionsResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val info = manageOAuthConnectionUseCase.getConnections(memberId)

        return ResponseEntity.ok(ApiResponse.success(info.toResponse()))
    }

    /**
     * OAuth 연결 추가
     */
    @PostMapping("/me/oauth-connections")
    suspend fun connectOAuth(
        @RequestBody request: ConnectOAuthRequest,
    ): ResponseEntity<ApiResponse<OAuthConnectionResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val connection = manageOAuthConnectionUseCase.connect(
            memberId = memberId,
            command = ConnectOAuthCommand(
                provider = request.provider,
                accessToken = request.accessToken,
            ),
        )

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ApiResponse.success(connection.toResponse()))
    }

    /**
     * OAuth 연결 해제
     */
    @DeleteMapping("/me/oauth-connections/{provider}")
    suspend fun disconnectOAuth(
        @PathVariable provider: OAuthProvider,
    ): ResponseEntity<Unit> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        manageOAuthConnectionUseCase.disconnect(
            memberId = memberId,
            provider = provider,
        )

        return ResponseEntity.noContent().build()
    }
}
