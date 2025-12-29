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
