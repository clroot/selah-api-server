package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.AggregateRoot
import io.clroot.selah.domains.member.domain.event.OAuthConnectedEvent
import java.time.LocalDateTime

/**
 * OAuth 연결 정보 Aggregate Root
 *
 * Member와 별도의 Aggregate로 관리되며, memberId를 통해 소속을 식별합니다.
 * 독립적인 생명주기를 가지며, 로그인/연동 관리 시 개별 조회가 가능합니다.
 *
 * 불변식: 동일 Member에 대해 동일 Provider는 하나만 존재 (DB Unique 제약으로 보장)
 */
class OAuthConnection(
    id: OAuthConnectionId,
    // --- 비즈니스 필드 ---
    val memberId: MemberId,
    val provider: OAuthProvider,
    val providerId: String,
    val connectedAt: LocalDateTime,
    // --- 메타 필드 (하단) ---
    version: Long?,
    createdAt: LocalDateTime,
    updatedAt: LocalDateTime,
) : AggregateRoot<OAuthConnectionId>(id, version, createdAt, updatedAt) {

    init {
        require(providerId.isNotBlank()) { "Provider ID must not be blank" }
    }

    companion object {
        fun create(memberId: MemberId, provider: OAuthProvider, providerId: String): OAuthConnection {
            val now = LocalDateTime.now()
            val connection = OAuthConnection(
                id = OAuthConnectionId.new(),
                memberId = memberId,
                provider = provider,
                providerId = providerId,
                connectedAt = now,
                version = null,
                createdAt = now,
                updatedAt = now,
            )
            connection.registerEvent(OAuthConnectedEvent(connection))
            return connection
        }
    }
}
