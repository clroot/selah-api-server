package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException

/**
 * 현재 회원 정보 조회 UseCase
 */
interface GetCurrentMemberUseCase {
    /**
     * ID로 회원 정보를 조회합니다.
     *
     * @param memberId 회원 ID
     * @return 회원 정보
     * @throws MemberNotFoundException 회원이 존재하지 않는 경우
     */
    suspend fun getMember(memberId: MemberId): Member

    /**
     * 프로필을 업데이트합니다.
     *
     * @param memberId 회원 ID
     * @param command 업데이트 정보
     * @return 업데이트된 회원 정보
     */
    suspend fun updateProfile(
        memberId: MemberId,
        command: UpdateProfileCommand,
    ): Member
}

/**
 * 프로필 업데이트 Command
 */
data class UpdateProfileCommand(
    val nickname: String?,
    val profileImageUrl: String?,
)
