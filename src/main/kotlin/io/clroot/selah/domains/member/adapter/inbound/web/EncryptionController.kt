package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.member.adapter.inbound.web.dto.*
import io.clroot.selah.domains.member.application.port.inbound.ManageEncryptionSettingsUseCase
import io.clroot.selah.domains.member.application.port.inbound.SetupEncryptionCommand
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * E2E 암호화 설정 Controller
 */
@RestController
@RequestMapping("/api/v1/encryption")
class EncryptionController(
    private val manageEncryptionSettingsUseCase: ManageEncryptionSettingsUseCase,
) {

    /**
     * 암호화 설정 초기화
     */
    @PostMapping("/setup")
    suspend fun setup(
        @RequestBody request: SetupEncryptionRequest,
    ): ResponseEntity<ApiResponse<SetupEncryptionResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()

        val settings = manageEncryptionSettingsUseCase.setup(
            memberId = memberId,
            command = SetupEncryptionCommand(
                salt = request.salt,
                recoveryKeyHash = request.recoveryKeyHash,
            )
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(settings.toSetupResponse()))
    }

    /**
     * 암호화 설정 조회
     */
    @GetMapping("/settings")
    suspend fun getSettings(): ResponseEntity<ApiResponse<EncryptionSettingsResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val settings = manageEncryptionSettingsUseCase.getSettings(memberId)

        return ResponseEntity.ok(ApiResponse.success(settings.toResponse()))
    }

    /**
     * 복구 키 검증
     */
    @PostMapping("/verify-recovery")
    suspend fun verifyRecoveryKey(
        @RequestBody request: VerifyRecoveryKeyRequest,
    ): ResponseEntity<ApiResponse<VerifyRecoveryKeyResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()

        val isValid = manageEncryptionSettingsUseCase.verifyRecoveryKey(
            memberId = memberId,
            recoveryKeyHash = request.recoveryKeyHash,
        )

        return ResponseEntity.ok(ApiResponse.success(VerifyRecoveryKeyResponse(valid = isValid)))
    }

    /**
     * 암호화 설정 삭제
     */
    @DeleteMapping("/settings")
    suspend fun deleteSettings(): ResponseEntity<ApiResponse<Unit>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        manageEncryptionSettingsUseCase.deleteSettings(memberId)

        return ResponseEntity.ok(ApiResponse.success(Unit))
    }
}
