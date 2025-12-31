package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.ServerKey

/**
 * Server Key 조회 Port
 */
interface LoadServerKeyPort {
    /**
     * 회원 ID로 Server Key 조회
     */
    suspend fun findByMemberId(memberId: MemberId): ServerKey?

    /**
     * 회원의 Server Key 존재 여부 확인
     */
    suspend fun existsByMemberId(memberId: MemberId): Boolean
}
