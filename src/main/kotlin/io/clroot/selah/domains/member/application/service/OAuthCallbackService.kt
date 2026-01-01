package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.ConnectOAuthCommand
import io.clroot.selah.domains.member.application.port.inbound.GetAuthorizationUrlCommand
import io.clroot.selah.domains.member.application.port.inbound.LoginResult
import io.clroot.selah.domains.member.application.port.inbound.LoginWithOAuthCommand
import io.clroot.selah.domains.member.application.port.inbound.LoginUseCase
import io.clroot.selah.domains.member.application.port.inbound.ManageOAuthConnectionUseCase
import io.clroot.selah.domains.member.application.port.inbound.OAuthCallbackCommand
import io.clroot.selah.domains.member.application.port.inbound.OAuthCallbackUseCase
import io.clroot.selah.domains.member.application.port.outbound.OAuthTokenPort
import io.clroot.selah.domains.member.application.port.outbound.OAuthUserInfoPort
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service

/**
 * OAuth 콜백 처리 Service
 */
@Service
class OAuthCallbackService(
    private val oAuthTokenPort: OAuthTokenPort,
    private val oAuthUserInfoPort: OAuthUserInfoPort,
    private val loginUseCase: LoginUseCase,
    private val manageOAuthConnectionUseCase: ManageOAuthConnectionUseCase,
) : OAuthCallbackUseCase {

    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    override fun getAuthorizationUrl(command: GetAuthorizationUrlCommand): String {
        val redirectUri = oAuthTokenPort.getCallbackUri(command.provider)
        return oAuthTokenPort.buildAuthorizationUrl(
            provider = command.provider,
            redirectUri = redirectUri,
            state = command.state,
        )
    }

    override suspend fun handleCallback(command: OAuthCallbackCommand): LoginResult {
        logger.debug { "Processing OAuth callback for provider: ${command.provider}" }

        // 1. Authorization code를 Access Token으로 교환
        val redirectUri = oAuthTokenPort.getCallbackUri(command.provider)
        val tokenResult = oAuthTokenPort.exchangeCodeForToken(
            provider = command.provider,
            code = command.code,
            redirectUri = redirectUri,
        )

        // 2. Access Token으로 사용자 정보 조회
        val userInfo = oAuthUserInfoPort.getUserInfo(
            provider = command.provider,
            accessToken = tokenResult.accessToken,
        )

        logger.debug { "OAuth user info retrieved: providerId=${userInfo.providerId}" }

        // 3. 로그인 처리 (자동 회원가입 포함)
        return loginUseCase.loginWithOAuth(
            LoginWithOAuthCommand(
                email = Email(userInfo.email ?: "${userInfo.providerId}@${command.provider.name.lowercase()}.oauth"),
                nickname = userInfo.name ?: "User",
                provider = command.provider,
                providerId = userInfo.providerId,
                profileImageUrl = userInfo.profileImageUrl,
                userAgent = command.userAgent,
                ipAddress = command.ipAddress,
            )
        )
    }

    override suspend fun handleLinkCallback(memberId: MemberId, provider: OAuthProvider, code: String) {
        logger.debug { "Processing OAuth link callback for provider: $provider, memberId: ${memberId.value}" }

        // 1. Authorization code를 Access Token으로 교환
        val redirectUri = oAuthTokenPort.getCallbackUri(provider)
        val tokenResult = oAuthTokenPort.exchangeCodeForToken(
            provider = provider,
            code = code,
            redirectUri = redirectUri,
        )

        // 2. 기존 계정에 OAuth 연결
        manageOAuthConnectionUseCase.connect(
            memberId = memberId,
            command = ConnectOAuthCommand(
                provider = provider,
                accessToken = tokenResult.accessToken,
            ),
        )

        logger.info { "OAuth connection linked: memberId=${memberId.value}, provider=$provider" }
    }
}
