package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider

/**
 * OAuth 인증 흐름을 처리하는 UseCase
 */
interface OAuthCallbackUseCase {
    /**
     * OAuth Authorization URL을 생성합니다.
     *
     * @param command Authorization URL 생성 정보
     * @return Authorization URL
     */
    fun getAuthorizationUrl(command: GetAuthorizationUrlCommand): String

    /**
     * OAuth Callback을 처리하고 로그인합니다.
     *
     * @param command Callback 처리 정보
     * @return 로그인 결과
     */
    suspend fun handleCallback(command: OAuthCallbackCommand): LoginResult

    /**
     * OAuth Callback을 처리하고 기존 계정에 연결합니다.
     *
     * @param memberId 현재 로그인한 사용자 ID
     * @param provider OAuth Provider
     * @param code Authorization Code
     */
    suspend fun handleLinkCallback(memberId: MemberId, provider: OAuthProvider, code: String)
}

/**
 * Authorization URL 생성 Command
 */
data class GetAuthorizationUrlCommand(
    val provider: OAuthProvider,
    val state: String,
)

/**
 * OAuth Callback Command
 */
data class OAuthCallbackCommand(
    val provider: OAuthProvider,
    val code: String,
    val state: String,
    val userAgent: String?,
    val ipAddress: String?,
)
