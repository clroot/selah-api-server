package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId

/**
 * 로그아웃 UseCase
 */
interface LogoutUseCase {
    /**
     * 현재 세션에서 로그아웃합니다.
     *
     * @param sessionToken 세션 토큰
     */
    suspend fun logout(sessionToken: String)

    /**
     * 모든 세션에서 로그아웃합니다. (전체 로그아웃)
     *
     * @param memberId 회원 ID
     */
    suspend fun logoutAll(memberId: MemberId)
}
