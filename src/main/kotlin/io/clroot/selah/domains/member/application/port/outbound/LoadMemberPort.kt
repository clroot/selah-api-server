package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider

/**
 * Member 조회를 위한 Outbound Port
 */
interface LoadMemberPort {
    /**
     * ID로 Member를 조회합니다.
     *
     * @param memberId Member ID
     * @return Member 또는 null
     */
    suspend fun findById(memberId: MemberId): Member?

    /**
     * 이메일로 Member를 조회합니다.
     *
     * @param email 이메일
     * @return Member 또는 null
     */
    suspend fun findByEmail(email: Email): Member?

    /**
     * OAuth Provider ID로 Member를 조회합니다.
     *
     * @param provider OAuth Provider
     * @param providerId Provider에서 제공하는 사용자 ID
     * @return Member 또는 null
     */
    suspend fun findByOAuthConnection(provider: OAuthProvider, providerId: String): Member?

    /**
     * 이메일 존재 여부를 확인합니다.
     *
     * @param email 이메일
     * @return 존재 여부
     */
    suspend fun existsByEmail(email: Email): Boolean
}
