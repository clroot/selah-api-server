package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import io.clroot.selah.domains.member.domain.OAuthProvider
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * OAuthConnection JPA Entity
 *
 * Member와 1:N 관계를 가지며, Member Aggregate 내부에서 관리됩니다.
 * Provider + ProviderId 조합으로 OAuth 사용자를 조회할 수 있습니다.
 */
@Entity
@Table(
    name = "oauth_connections",
    indexes = [
        Index(name = "idx_oauth_provider_id", columnList = "provider, provider_id", unique = true),
    ],
    uniqueConstraints = [
        UniqueConstraint(
            name = "uk_member_provider",
            columnNames = ["member_id", "provider"],
        ),
    ],
)
class OAuthConnectionEntity(
    @Id
    @Column(name = "id", length = 26)
    val id: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 20)
    val provider: OAuthProvider,
    @Column(name = "provider_id", nullable = false)
    val providerId: String,
    @Column(name = "connected_at", nullable = false)
    val connectedAt: LocalDateTime,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    var member: MemberEntity? = null,
)
