package io.clroot.selah.domains.member.application.service

import io.clroot.selah.common.application.publishAndClearEvents
import io.clroot.selah.domains.member.application.port.inbound.ConnectOAuthCommand
import io.clroot.selah.domains.member.application.port.inbound.ManageOAuthConnectionUseCase
import io.clroot.selah.domains.member.application.port.inbound.OAuthConnectionInfo
import io.clroot.selah.domains.member.application.port.inbound.OAuthConnectionsInfo
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.OAuthUserInfoPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import io.clroot.selah.domains.member.domain.exception.OAuthAlreadyLinkedToAnotherMemberException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * OAuth 연결 관리 Service
 */
@Service
@Transactional
class OAuthConnectionService(
    private val loadMemberPort: LoadMemberPort,
    private val saveMemberPort: SaveMemberPort,
    private val oauthUserInfoPort: OAuthUserInfoPort,
    private val eventPublisher: ApplicationEventPublisher,
) : ManageOAuthConnectionUseCase {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    @Transactional(readOnly = true)
    override suspend fun getConnections(memberId: MemberId): OAuthConnectionsInfo {
        val member =
            loadMemberPort.findById(memberId)
                ?: throw MemberNotFoundException(memberId.value)

        val connections =
            member.oauthConnections.map { connection ->
                OAuthConnectionInfo(
                    provider = connection.provider,
                    connectedAt = connection.connectedAt,
                )
            }

        val availableProviders =
            OAuthProvider.entries
                .filter { provider -> !member.hasProvider(provider) }

        return OAuthConnectionsInfo(
            connections = connections,
            availableProviders = availableProviders,
        )
    }

    override suspend fun connect(
        memberId: MemberId,
        command: ConnectOAuthCommand,
    ): OAuthConnectionInfo {
        val member =
            loadMemberPort.findById(memberId)
                ?: throw MemberNotFoundException(memberId.value)

        // OAuth Provider에서 사용자 정보 가져오기
        val oauthUserInfo = oauthUserInfoPort.getUserInfo(command.provider, command.accessToken)

        logger.info { "OAuth user info retrieved for ${command.provider}: providerId=${oauthUserInfo.providerId}" }

        // 다른 계정에 이미 연결되어 있는지 확인
        val existingMember = loadMemberPort.findByOAuthConnection(command.provider, oauthUserInfo.providerId)
        if (existingMember != null && existingMember.id != memberId) {
            throw OAuthAlreadyLinkedToAnotherMemberException(command.provider.name)
        }

        // OAuth 연결
        member.connectOAuth(
            provider = command.provider,
            providerId = oauthUserInfo.providerId,
        )

        val savedMember = saveMemberPort.save(member)
        savedMember.publishAndClearEvents(eventPublisher)

        val connection = savedMember.oauthConnections.find { it.provider == command.provider }!!

        logger.info { "OAuth connection added: memberId=${memberId.value}, provider=${command.provider}" }

        return OAuthConnectionInfo(
            provider = connection.provider,
            connectedAt = connection.connectedAt,
        )
    }

    override suspend fun disconnect(
        memberId: MemberId,
        provider: OAuthProvider,
    ) {
        val member =
            loadMemberPort.findById(memberId)
                ?: throw MemberNotFoundException(memberId.value)

        // OAuth 연결 해제
        member.disconnectOAuth(provider)

        val savedMember = saveMemberPort.save(member)
        savedMember.publishAndClearEvents(eventPublisher)

        logger.info { "OAuth connection removed: memberId=${memberId.value}, provider=$provider" }
    }
}
