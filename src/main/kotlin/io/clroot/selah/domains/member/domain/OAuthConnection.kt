package io.clroot.selah.domains.member.domain

import io.clroot.selah.common.domain.DomainEntity
import java.time.LocalDateTime

/**
 * OAuth 연결 정보 Entity
 *
 * Member Aggregate 내부에서 관리되는 Entity입니다.
 * Member를 통해서만 생성, 조회, 삭제가 가능합니다.
 *
 * 불변식: 동일 Member에 대해 동일 Provider는 하나만 존재 (Member에서 검증)
 */
class OAuthConnection(
    override val id: OAuthConnectionId,
    val provider: OAuthProvider,
    val providerId: String,
    val connectedAt: LocalDateTime,
) : DomainEntity<OAuthConnectionId>(id) {

    init {
        require(providerId.isNotBlank()) { "Provider ID must not be blank" }
    }

    companion object {
        fun create(
            provider: OAuthProvider,
            providerId: String,
        ): OAuthConnection = OAuthConnection(
            id = OAuthConnectionId.new(),
            provider = provider,
            providerId = providerId,
            connectedAt = LocalDateTime.now(),
        )
    }
}
