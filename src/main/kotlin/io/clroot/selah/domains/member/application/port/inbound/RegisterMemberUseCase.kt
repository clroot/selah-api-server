package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.*

/**
 * 회원가입 UseCase
 */
interface RegisterMemberUseCase {
    /**
     * 이메일/비밀번호로 회원가입합니다.
     *
     * @param command 회원가입 정보
     * @return 가입된 회원 정보
     * @throws EmailAlreadyExistsException 이메일이 이미 존재하는 경우
     */
    suspend fun registerWithEmail(command: RegisterWithEmailCommand): Member

    /**
     * OAuth로 회원가입 또는 로그인합니다.
     * 기존 회원이면 로그인, 신규면 자동 가입됩니다.
     *
     * @param command OAuth 정보
     * @return 회원 정보와 신규 가입 여부
     */
    suspend fun registerOrLoginWithOAuth(command: RegisterWithOAuthCommand): OAuthRegisterResult
}

/**
 * 이메일/비밀번호 회원가입 Command
 */
data class RegisterWithEmailCommand(
    val email: Email,
    val nickname: String,
    val password: NewPassword,
)

/**
 * OAuth 회원가입 Command
 */
data class RegisterWithOAuthCommand(
    val email: Email,
    val nickname: String,
    val provider: OAuthProvider,
    val providerId: String,
    val profileImageUrl: String? = null,
)

/**
 * OAuth 회원가입/로그인 결과
 */
data class OAuthRegisterResult(
    val member: Member,
    val isNewMember: Boolean,
)
