package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.OAuthProvider
import io.clroot.selah.domains.member.domain.exception.OAuthTokenValidationFailedException

/**
 * OAuth Provider로부터 사용자 정보를 가져오는 Port
 */
interface OAuthUserInfoPort {
    /**
     * Access Token을 사용하여 OAuth Provider로부터 사용자 정보를 가져옵니다.
     *
     * @param provider OAuth Provider
     * @param accessToken OAuth Access Token
     * @return 사용자 정보
     * @throws OAuthTokenValidationFailedException 토큰 검증 실패 시
     */
    suspend fun getUserInfo(provider: OAuthProvider, accessToken: String): OAuthUserInfo
}

/**
 * OAuth Provider로부터 가져온 사용자 정보
 */
data class OAuthUserInfo(
    val providerId: String,
    val email: String?,
    val name: String?,
    val profileImageUrl: String?,
)
