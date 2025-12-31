package io.clroot.selah.domains.member.domain.exception

/**
 * Member 도메인 예외의 기본 클래스
 */
sealed class MemberException(
    val code: String,
    override val message: String,
) : RuntimeException(message)

/**
 * 이메일이 이미 존재하는 경우
 */
class EmailAlreadyExistsException(email: String) : MemberException(
    code = "EMAIL_ALREADY_EXISTS",
    message = "Email already exists: $email",
)

/**
 * 회원을 찾을 수 없는 경우
 */
class MemberNotFoundException(identifier: String) : MemberException(
    code = "MEMBER_NOT_FOUND",
    message = "Member not found: $identifier",
)

/**
 * 잘못된 인증 정보 (비밀번호 불일치 등)
 * 보안상 구체적인 이유를 노출하지 않음
 */
class InvalidCredentialsException : MemberException(
    code = "INVALID_CREDENTIALS",
    message = "Invalid email or password",
)

/**
 * 이메일 인증이 필요한 경우
 */
class EmailNotVerifiedException(email: String) : MemberException(
    code = "EMAIL_NOT_VERIFIED",
    message = "Email not verified: $email",
)

/**
 * 세션이 만료된 경우
 */
class SessionExpiredException : MemberException(
    code = "SESSION_EXPIRED",
    message = "Session has expired",
)

/**
 * 유효하지 않은 세션인 경우
 */
class InvalidSessionException : MemberException(
    code = "INVALID_SESSION",
    message = "Invalid session",
)

/**
 * 유효하지 않은 API Key인 경우
 */
class InvalidApiKeyException : MemberException(
    code = "INVALID_API_KEY",
    message = "Invalid API key",
)

/**
 * OAuth Provider가 이미 연결된 경우
 */
class OAuthProviderAlreadyConnectedException(provider: String) : MemberException(
    code = "OAUTH_PROVIDER_ALREADY_CONNECTED",
    message = "OAuth provider already connected: $provider",
)

/**
 * OAuth Provider가 연결되지 않은 경우
 */
class OAuthProviderNotConnectedException(provider: String) : MemberException(
    code = "OAUTH_PROVIDER_NOT_CONNECTED",
    message = "OAuth provider not connected: $provider",
)

/**
 * 암호화 설정을 찾을 수 없는 경우
 */
class EncryptionSettingsNotFoundException(memberId: String) : MemberException(
    code = "ENCRYPTION_SETTINGS_NOT_FOUND",
    message = "Encryption settings not found for member: $memberId",
)

/**
 * 암호화가 이미 설정된 경우
 */
class EncryptionAlreadySetupException(memberId: String) : MemberException(
    code = "ENCRYPTION_ALREADY_SETUP",
    message = "Encryption already setup for member: $memberId",
)

/**
 * 이메일 인증 토큰이 유효하지 않은 경우
 */
class InvalidEmailVerificationTokenException : MemberException(
    code = "INVALID_EMAIL_VERIFICATION_TOKEN",
    message = "Invalid or expired verification token",
)

/**
 * 이메일 인증 토큰이 만료된 경우
 */
class EmailVerificationTokenExpiredException : MemberException(
    code = "EMAIL_VERIFICATION_TOKEN_EXPIRED",
    message = "Verification token has expired",
)

/**
 * 이메일이 이미 인증된 경우
 */
class EmailAlreadyVerifiedException(email: String) : MemberException(
    code = "EMAIL_ALREADY_VERIFIED",
    message = "Email already verified: $email",
)

/**
 * 이메일 인증 재발송 대기 시간 내에 재요청한 경우
 */
class EmailVerificationResendTooSoonException(
    val remainingSeconds: Long,
) : MemberException(
    code = "EMAIL_VERIFICATION_RESEND_TOO_SOON",
    message = "Please wait $remainingSeconds seconds before requesting another verification email",
)

/**
 * 유효하지 않은 비밀번호 재설정 토큰인 경우
 */
class InvalidPasswordResetTokenException : MemberException(
    code = "INVALID_PASSWORD_RESET_TOKEN",
    message = "Invalid or expired password reset token",
)

/**
 * 비밀번호 재설정 토큰 재발송 대기 시간 내에 재요청한 경우
 */
class PasswordResetResendTooSoonException(
    val remainingSeconds: Long,
) : MemberException(
    code = "PASSWORD_RESET_RESEND_TOO_SOON",
    message = "Please wait $remainingSeconds seconds before requesting another password reset email",
)

/**
 * OAuth 토큰 검증 실패
 */
class OAuthTokenValidationFailedException(provider: String) : MemberException(
    code = "OAUTH_TOKEN_VALIDATION_FAILED",
    message = "Failed to validate OAuth token for provider: $provider",
)

/**
 * OAuth 토큰 교환 실패
 */
class OAuthTokenExchangeFailedException(provider: String) : MemberException(
    code = "OAUTH_TOKEN_EXCHANGE_FAILED",
    message = "Failed to exchange OAuth token for provider: $provider",
)

/**
 * OAuth 상태 검증 실패 (CSRF 방지)
 */
class OAuthStateValidationFailedException : MemberException(
    code = "OAUTH_STATE_VALIDATION_FAILED",
    message = "OAuth state validation failed",
)

/**
 * OAuth가 이미 다른 계정에 연결된 경우
 */
class OAuthAlreadyLinkedToAnotherMemberException(provider: String) : MemberException(
    code = "OAUTH_ALREADY_LINKED_TO_ANOTHER_MEMBER",
    message = "This $provider account is already linked to another member",
)

/**
 * 마지막 로그인 방법을 해제하려고 시도한 경우
 */
class CannotDisconnectLastLoginMethodException : MemberException(
    code = "CANNOT_DISCONNECT_LAST_LOGIN_METHOD",
    message = "Cannot disconnect the last login method. Please set a password or connect another OAuth provider first.",
)
