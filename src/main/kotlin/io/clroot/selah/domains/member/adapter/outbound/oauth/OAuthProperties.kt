package io.clroot.selah.domains.member.adapter.outbound.oauth

import io.clroot.selah.domains.member.domain.OAuthProvider
import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "selah.oauth")
data class OAuthProperties(
    val backendBaseUrl: String,
    val frontendCallbackUrl: String,
    val providers: Map<String, ProviderConfig>,
) {
    data class ProviderConfig(
        val clientId: String,
        val clientSecret: String,
        val authUrl: String,
        val tokenUrl: String,
        val scope: String,
    )

    fun getProviderConfig(provider: OAuthProvider): ProviderConfig {
        return providers[provider.name.lowercase()]
            ?: throw IllegalArgumentException("Unknown OAuth provider: ${provider.name}")
    }
}
