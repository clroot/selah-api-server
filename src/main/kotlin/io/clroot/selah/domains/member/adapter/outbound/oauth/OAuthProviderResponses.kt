package io.clroot.selah.domains.member.adapter.outbound.oauth

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty

/**
 * Google OAuth2 UserInfo 응답
 * https://www.googleapis.com/oauth2/v2/userinfo
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class GoogleUserInfoResponse(
    val id: String,
    val email: String?,
    val name: String?,
    val picture: String?,
    @JsonProperty("verified_email")
    val verifiedEmail: Boolean?,
)

/**
 * Kakao OAuth2 UserInfo 응답
 * https://kapi.kakao.com/v2/user/me
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class KakaoUserInfoResponse(
    val id: Long,
    @JsonProperty("kakao_account")
    val kakaoAccount: KakaoAccount?,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class KakaoAccount(
        val email: String?,
        val profile: KakaoProfile?,
    )

    @JsonIgnoreProperties(ignoreUnknown = true)
    data class KakaoProfile(
        val nickname: String?,
        @JsonProperty("profile_image_url")
        val profileImageUrl: String?,
    )
}

/**
 * Naver OAuth2 UserInfo 응답
 * https://openapi.naver.com/v1/nid/me
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class NaverUserInfoResponse(
    val resultcode: String?,
    val message: String?,
    val response: NaverUserInfo?,
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    data class NaverUserInfo(
        val id: String,
        val email: String?,
        val name: String?,
        val nickname: String?,
        @JsonProperty("profile_image")
        val profileImage: String?,
    )
}
