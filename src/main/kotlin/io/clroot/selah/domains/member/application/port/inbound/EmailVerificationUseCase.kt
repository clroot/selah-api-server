package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.EmailAlreadyVerifiedException
import io.clroot.selah.domains.member.domain.exception.EmailVerificationResendTooSoonException
import io.clroot.selah.domains.member.domain.exception.InvalidEmailVerificationTokenException
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException

/**
 * 이메일 인증 UseCase
 */
interface EmailVerificationUseCase {
    /**
     * 이메일 인증 메일을 발송합니다.
     * 기존 토큰이 있으면 무효화하고 새 토큰을 생성합니다.
     *
     * @param command 발송 요청 정보
     * @throws MemberNotFoundException 회원이 존재하지 않는 경우
     * @throws EmailAlreadyVerifiedException 이미 인증된 이메일인 경우
     * @throws EmailVerificationResendTooSoonException 재발송 대기 시간 내인 경우
     */
    suspend fun sendVerificationEmail(command: SendVerificationEmailCommand)

    /**
     * 이메일 인증을 완료합니다.
     *
     * @param command 인증 요청 정보
     * @return 인증 완료된 회원 정보
     * @throws InvalidEmailVerificationTokenException 유효하지 않은 토큰인 경우
     */
    suspend fun verifyEmail(command: VerifyEmailCommand): Member
}

/**
 * 이메일 인증 메일 발송 Command
 */
data class SendVerificationEmailCommand(
    val memberId: MemberId,
)

/**
 * 이메일 인증 Command
 */
data class VerifyEmailCommand(
    val token: String,
)
