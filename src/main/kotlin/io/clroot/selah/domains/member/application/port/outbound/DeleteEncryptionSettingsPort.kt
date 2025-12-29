package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.MemberId

/**
 * 암호화 설정 삭제 Port
 */
interface DeleteEncryptionSettingsPort {
    /**
     * 회원 ID로 암호화 설정 삭제
     */
    suspend fun deleteByMemberId(memberId: MemberId)
}
