package io.clroot.selah.domains.member.adapter.outbound.oauth

import com.fasterxml.jackson.annotation.JsonProperty
import io.clroot.selah.domains.member.application.port.outbound.OAuthTokenPort
import io.clroot.selah.domains.member.application.port.outbound.OAuthTokenResult
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.clroot.selah.domains.member.domain.exception.OAuthTokenExchangeFailedException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient
import org.springframework.web.client.body
import org.springframework.web.util.UriComponentsBuilder

/**
 * OAuth Provider와 Token 교환을 수행하는 Adapter
 */
@Component
class OAuthTokenAdapter(
    private val oAuthProperties: OAuthProperties,
) : OAuthTokenPort {

    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    private val restClient = RestClient.builder()
        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
        .build()

    override suspend fun exchangeCodeForToken(
        provider: OAuthProvider,
        code: String,
        redirectUri: String,
    ): OAuthTokenResult {
        return withContext(Dispatchers.IO) {
            val config = oAuthProperties.getProviderConfig(provider)

            try {
                val formData = LinkedMultiValueMap<String, String>().apply {
                    add("grant_type", "authorization_code")
                    add("client_id", config.clientId)
                    add("client_secret", config.clientSecret)
                    add("redirect_uri", redirectUri)
                    add("code", code)
                }

                val response = restClient.post()
                    .uri(config.tokenUrl)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(formData)
                    .retrieve()
                    .body<TokenResponse>()
                    ?: throw OAuthTokenExchangeFailedException(provider.name)

                logger.debug { "Token exchanged successfully for provider: $provider" }

                OAuthTokenResult(
                    accessToken = response.accessToken,
                    tokenType = response.tokenType ?: "Bearer",
                    expiresIn = response.expiresIn,
                    refreshToken = response.refreshToken,
                )
            } catch (e: OAuthTokenExchangeFailedException) {
                throw e
            } catch (e: Exception) {
                logger.error(e) { "Failed to exchange token for provider: $provider" }
                throw OAuthTokenExchangeFailedException(provider.name)
            }
        }
    }

    override fun buildAuthorizationUrl(
        provider: OAuthProvider,
        redirectUri: String,
        state: String,
    ): String {
        val config = oAuthProperties.getProviderConfig(provider)

        val builder = UriComponentsBuilder.fromUriString(config.authUrl)
            .queryParam("client_id", config.clientId)
            .queryParam("redirect_uri", redirectUri)
            .queryParam("response_type", "code")
            .queryParam("state", state)

        if (config.scope.isNotBlank()) {
            builder.queryParam("scope", config.scope)
        }

        // Google 특수 파라미터
        if (provider == OAuthProvider.GOOGLE) {
            builder.queryParam("access_type", "offline")
            builder.queryParam("prompt", "select_account")
        }

        return builder.build().encode().toUriString()
    }
}

/**
 * OAuth Token 응답 DTO
 */
data class TokenResponse(
    @JsonProperty("access_token")
    val accessToken: String,
    @JsonProperty("token_type")
    val tokenType: String?,
    @JsonProperty("expires_in")
    val expiresIn: Int?,
    @JsonProperty("refresh_token")
    val refreshToken: String?,
)
