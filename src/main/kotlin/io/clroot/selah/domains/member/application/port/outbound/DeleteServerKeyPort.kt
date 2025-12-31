package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.MemberId

/**
 * Server Key 삭제 Port
 */
interface DeleteServerKeyPort {
    /**
     * 회원 ID로 Server Key 삭제
     */
    suspend fun deleteByMemberId(memberId: MemberId)
}
