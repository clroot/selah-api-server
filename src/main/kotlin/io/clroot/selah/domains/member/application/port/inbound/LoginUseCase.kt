package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.application.port.outbound.SessionInfo
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.clroot.selah.domains.member.domain.RawPassword
import io.clroot.selah.domains.member.domain.exception.EmailNotVerifiedException
import io.clroot.selah.domains.member.domain.exception.InvalidCredentialsException

/**
 * 로그인 UseCase
 */
interface LoginUseCase {
    /**
     * 이메일/비밀번호로 로그인합니다.
     *
     * @param command 로그인 정보
     * @return 로그인 결과 (세션 정보 포함)
     * @throws InvalidCredentialsException 인증 실패
     * @throws EmailNotVerifiedException 이메일 미인증
     */
    suspend fun loginWithEmail(command: LoginWithEmailCommand): LoginResult

    /**
     * OAuth로 로그인합니다.
     * 기존 회원이 아니면 자동으로 가입됩니다.
     *
     * @param command OAuth 정보
     * @return 로그인 결과
     */
    suspend fun loginWithOAuth(command: LoginWithOAuthCommand): LoginResult
}

/**
 * 이메일 로그인 Command
 */
data class LoginWithEmailCommand(
    val email: Email,
    val password: RawPassword,
    val userAgent: String?,
    val ipAddress: String?,
)

/**
 * OAuth 로그인 Command
 */
data class LoginWithOAuthCommand(
    val email: Email,
    val nickname: String,
    val provider: OAuthProvider,
    val providerId: String,
    val profileImageUrl: String?,
    val userAgent: String?,
    val ipAddress: String?,
)

/**
 * 로그인 결과
 */
data class LoginResult(
    val session: SessionInfo,
    val memberId: MemberId,
    val nickname: String,
    val isNewMember: Boolean,
)
