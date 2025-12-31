package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.common.response.ApiResponse
import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.member.adapter.inbound.web.dto.*
import io.clroot.selah.domains.member.application.port.inbound.ManageEncryptionSettingsUseCase
import io.clroot.selah.domains.member.application.port.inbound.SetupEncryptionCommand
import io.clroot.selah.domains.member.application.port.inbound.UpdateEncryptionCommand
import io.clroot.selah.domains.member.application.port.inbound.UpdateRecoveryKeyCommand
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * E2E 암호화 설정 Controller
 *
 * DEK/KEK 구조 기반의 암호화 설정 API를 제공합니다.
 */
@RestController
@RequestMapping("/api/v1/encryption")
class EncryptionController(
    private val manageEncryptionSettingsUseCase: ManageEncryptionSettingsUseCase,
) {

    /**
     * 암호화 설정 초기화 (회원가입 시)
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
                encryptedDEK = request.encryptedDEK,
                recoveryEncryptedDEK = request.recoveryEncryptedDEK,
                recoveryKeyHash = request.recoveryKeyHash,
            )
        )

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(settings.toSetupResponse()))
    }

    /**
     * 암호화 설정 조회 (로그인 시 DEK 복호화용)
     */
    @GetMapping("/settings")
    suspend fun getSettings(): ResponseEntity<ApiResponse<EncryptionSettingsResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val settings = manageEncryptionSettingsUseCase.getSettings(memberId)

        return ResponseEntity.ok(ApiResponse.success(settings.toResponse()))
    }

    /**
     * 복구용 설정 조회 (비밀번호 분실 시 DEK 복구용)
     */
    @GetMapping("/recovery-settings")
    suspend fun getRecoverySettings(): ResponseEntity<ApiResponse<RecoverySettingsResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()
        val result = manageEncryptionSettingsUseCase.getRecoverySettings(memberId)

        return ResponseEntity.ok(ApiResponse.success(result.toResponse()))
    }

    /**
     * 암호화 키 업데이트 (비밀번호 변경 시)
     */
    @PutMapping("/encryption")
    suspend fun updateEncryption(
        @RequestBody request: UpdateEncryptionRequest,
    ): ResponseEntity<ApiResponse<UpdateEncryptionResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()

        val settings = manageEncryptionSettingsUseCase.updateEncryption(
            memberId = memberId,
            command = UpdateEncryptionCommand(
                salt = request.salt,
                encryptedDEK = request.encryptedDEK,
            )
        )

        return ResponseEntity.ok(ApiResponse.success(settings.toUpdateEncryptionResponse()))
    }

    /**
     * 복구 키 재생성
     */
    @PutMapping("/recovery-key")
    suspend fun updateRecoveryKey(
        @RequestBody request: UpdateRecoveryKeyRequest,
    ): ResponseEntity<ApiResponse<UpdateRecoveryKeyResponse>> {
        val memberId = SecurityUtils.requireCurrentMemberId()

        val settings = manageEncryptionSettingsUseCase.updateRecoveryKey(
            memberId = memberId,
            command = UpdateRecoveryKeyCommand(
                recoveryEncryptedDEK = request.recoveryEncryptedDEK,
                recoveryKeyHash = request.recoveryKeyHash,
            )
        )

        return ResponseEntity.ok(ApiResponse.success(settings.toUpdateRecoveryKeyResponse()))
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
