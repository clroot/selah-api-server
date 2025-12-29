package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.EncryptionSettings

/**
 * 암호화 설정 저장 Port
 */
interface SaveEncryptionSettingsPort {
    /**
     * 암호화 설정 저장 (생성/수정)
     */
    suspend fun save(encryptionSettings: EncryptionSettings): EncryptionSettings
}
