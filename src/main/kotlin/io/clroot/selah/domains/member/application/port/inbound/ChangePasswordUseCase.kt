package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.InvalidCredentialsException
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import io.clroot.selah.domains.member.domain.exception.PasswordAlreadySetException
import io.clroot.selah.domains.member.domain.exception.PasswordNotSetException

/**
 * 비밀번호 변경/설정 UseCase
 */
interface ChangePasswordUseCase {
    /**
     * 비밀번호를 변경합니다. (기존 비밀번호가 있는 회원)
     *
     * @param memberId 회원 ID
     * @param command 비밀번호 변경 정보
     * @throws MemberNotFoundException 회원이 존재하지 않는 경우
     * @throws PasswordNotSetException 비밀번호가 설정되지 않은 회원인 경우
     * @throws InvalidCredentialsException 현재 비밀번호가 일치하지 않는 경우
     */
    suspend fun changePassword(
        memberId: MemberId,
        command: ChangePasswordCommand,
    )

    /**
     * 비밀번호를 설정합니다. (OAuth로 가입한 회원이 처음 비밀번호 설정)
     *
     * @param memberId 회원 ID
     * @param command 비밀번호 설정 정보
     * @throws MemberNotFoundException 회원이 존재하지 않는 경우
     * @throws PasswordAlreadySetException 이미 비밀번호가 설정된 회원인 경우
     */
    suspend fun setPassword(
        memberId: MemberId,
        command: SetPasswordCommand,
    )
}

/**
 * 비밀번호 변경 Command
 */
data class ChangePasswordCommand(
    val currentPassword: String,
    val newPassword: String,
)

/**
 * 비밀번호 설정 Command (OAuth 전용 계정)
 */
data class SetPasswordCommand(
    val newPassword: String,
)
