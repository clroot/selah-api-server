package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.EncryptionSettings
import io.clroot.selah.domains.member.domain.MemberId

/**
 * 암호화 설정 조회 Port
 */
interface LoadEncryptionSettingsPort {
    /**
     * 회원 ID로 암호화 설정 조회
     */
    suspend fun findByMemberId(memberId: MemberId): EncryptionSettings?

    /**
     * 회원의 암호화 설정 존재 여부 확인
     */
    suspend fun existsByMemberId(memberId: MemberId): Boolean
}
