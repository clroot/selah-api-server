package io.clroot.selah.domains.member.application.port.inbound

import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.clroot.selah.domains.member.domain.exception.*
import java.time.LocalDateTime

/**
 * OAuth 연결 관리 UseCase
 */
interface ManageOAuthConnectionUseCase {
    /**
     * 현재 회원의 OAuth 연결 목록을 조회합니다.
     */
    suspend fun getConnections(memberId: MemberId): OAuthConnectionsInfo

    /**
     * 새로운 OAuth Provider를 연결합니다.
     *
     * @throws OAuthTokenValidationFailedException 토큰 검증 실패 시
     * @throws OAuthProviderAlreadyConnectedException 이미 연결된 Provider인 경우
     * @throws OAuthAlreadyLinkedToAnotherMemberException 다른 계정에 이미 연결된 OAuth인 경우
     */
    suspend fun connect(memberId: MemberId, command: ConnectOAuthCommand): OAuthConnectionInfo

    /**
     * OAuth Provider 연결을 해제합니다.
     *
     * @throws OAuthProviderNotConnectedException 연결되지 않은 Provider인 경우
     * @throws CannotDisconnectLastLoginMethodException 마지막 로그인 방법인 경우
     */
    suspend fun disconnect(memberId: MemberId, provider: OAuthProvider)
}

/**
 * OAuth 연결 추가 Command
 */
data class ConnectOAuthCommand(
    val provider: OAuthProvider,
    val accessToken: String,
)

/**
 * OAuth 연결 목록 정보
 */
data class OAuthConnectionsInfo(
    val connections: List<OAuthConnectionInfo>,
    val availableProviders: List<OAuthProvider>,
)

/**
 * OAuth 연결 정보
 */
data class OAuthConnectionInfo(
    val provider: OAuthProvider,
    val connectedAt: LocalDateTime,
)
