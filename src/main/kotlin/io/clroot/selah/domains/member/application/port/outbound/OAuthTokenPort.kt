package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.OAuthProvider

/**
 * OAuth Provider로부터 Access Token을 교환하는 Port
 */
interface OAuthTokenPort {
    /**
     * Authorization Code를 Access Token으로 교환합니다.
     *
     * @param provider OAuth Provider
     * @param code Authorization Code
     * @param redirectUri Callback에 사용된 Redirect URI
     * @return Access Token
     */
    suspend fun exchangeCodeForToken(
        provider: OAuthProvider,
        code: String,
        redirectUri: String,
    ): OAuthTokenResult

    /**
     * Authorization URL을 생성합니다.
     *
     * @param provider OAuth Provider
     * @param redirectUri Callback URL
     * @param state CSRF 방지용 state
     * @return Authorization URL
     */
    fun buildAuthorizationUrl(
        provider: OAuthProvider,
        redirectUri: String,
        state: String,
    ): String

    /**
     * OAuth Callback URI를 반환합니다.
     *
     * @param provider OAuth Provider
     * @return Callback URI
     */
    fun getCallbackUri(provider: OAuthProvider): String
}

/**
 * OAuth Token 교환 결과
 */
data class OAuthTokenResult(
    val accessToken: String,
    val tokenType: String,
    val expiresIn: Int?,
    val refreshToken: String?,
)
