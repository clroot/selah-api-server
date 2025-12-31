package io.clroot.selah.domains.member.adapter.outbound.oauth

import io.clroot.selah.domains.member.application.port.outbound.OAuthUserInfo
import io.clroot.selah.domains.member.application.port.outbound.OAuthUserInfoPort
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.clroot.selah.domains.member.domain.exception.OAuthTokenValidationFailedException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

/**
 * OAuth Provider API를 호출하여 사용자 정보를 가져오는 Adapter
 */
@Component
class OAuthUserInfoAdapter : OAuthUserInfoPort {

    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}

        private const val GOOGLE_USERINFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo"
        private const val KAKAO_USERINFO_URL = "https://kapi.kakao.com/v2/user/me"
        private const val NAVER_USERINFO_URL = "https://openapi.naver.com/v1/nid/me"
    }

    private val restClient = RestClient.builder()
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build()

    override suspend fun getUserInfo(provider: OAuthProvider, accessToken: String): OAuthUserInfo {
        return withContext(Dispatchers.IO) {
            when (provider) {
                OAuthProvider.GOOGLE -> getGoogleUserInfo(accessToken)
                OAuthProvider.KAKAO -> getKakaoUserInfo(accessToken)
                OAuthProvider.NAVER -> getNaverUserInfo(accessToken)
            }
        }
    }

    private fun getGoogleUserInfo(accessToken: String): OAuthUserInfo {
        try {
            val response = restClient.get()
                .uri(GOOGLE_USERINFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body<GoogleUserInfoResponse>()
                ?: throw OAuthTokenValidationFailedException(OAuthProvider.GOOGLE.name)

            logger.debug { "Google user info retrieved: ${response.id}" }

            return OAuthUserInfo(
                providerId = response.id,
                email = response.email,
                name = response.name,
                profileImageUrl = response.picture,
            )
        } catch (e: OAuthTokenValidationFailedException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to get Google user info" }
            throw OAuthTokenValidationFailedException(OAuthProvider.GOOGLE.name)
        }
    }

    private fun getKakaoUserInfo(accessToken: String): OAuthUserInfo {
        try {
            val response = restClient.get()
                .uri(KAKAO_USERINFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body<KakaoUserInfoResponse>()
                ?: throw OAuthTokenValidationFailedException(OAuthProvider.KAKAO.name)

            logger.debug { "Kakao user info retrieved: ${response.id}" }

            return OAuthUserInfo(
                providerId = response.id.toString(),
                email = response.kakaoAccount?.email,
                name = response.kakaoAccount?.profile?.nickname,
                profileImageUrl = response.kakaoAccount?.profile?.profileImageUrl,
            )
        } catch (e: OAuthTokenValidationFailedException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to get Kakao user info" }
            throw OAuthTokenValidationFailedException(OAuthProvider.KAKAO.name)
        }
    }

    private fun getNaverUserInfo(accessToken: String): OAuthUserInfo {
        try {
            val response = restClient.get()
                .uri(NAVER_USERINFO_URL)
                .header(HttpHeaders.AUTHORIZATION, "Bearer $accessToken")
                .retrieve()
                .body<NaverUserInfoResponse>()
                ?: throw OAuthTokenValidationFailedException(OAuthProvider.NAVER.name)

            val naverResponse = response.response
                ?: throw OAuthTokenValidationFailedException(OAuthProvider.NAVER.name)

            logger.debug { "Naver user info retrieved: ${naverResponse.id}" }

            return OAuthUserInfo(
                providerId = naverResponse.id,
                email = naverResponse.email,
                name = naverResponse.name ?: naverResponse.nickname,
                profileImageUrl = naverResponse.profileImage,
            )
        } catch (e: OAuthTokenValidationFailedException) {
            throw e
        } catch (e: Exception) {
            logger.error(e) { "Failed to get Naver user info" }
            throw OAuthTokenValidationFailedException(OAuthProvider.NAVER.name)
        }
    }
}
