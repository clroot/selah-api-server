package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import io.clroot.selah.domains.member.domain.Member
import jakarta.persistence.*
import java.time.LocalDateTime

/**
 * Member JPA Entity
 *
 * Domain의 Member와 분리된 Persistence Layer Entity입니다.
 * JPA 어노테이션은 Adapter Layer에서만 사용합니다.
 */
@Entity
@Table(
    name = "members",
    indexes = [
        Index(name = "idx_members_email", columnList = "email", unique = true),
    ],
)
class MemberEntity(
    @Id
    @Column(name = "id", length = 26)
    val id: String,
    @Column(name = "email", nullable = false, unique = true)
    var email: String,
    @Column(name = "nickname", nullable = false)
    var nickname: String,
    @Column(name = "profile_image_url")
    var profileImageUrl: String? = null,
    @Column(name = "password_hash")
    var passwordHash: String? = null,
    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,
    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    var role: Member.Role = Member.Role.USER,
    @OneToMany(
        mappedBy = "member",
        cascade = [CascadeType.ALL],
        orphanRemoval = true,
        fetch = FetchType.LAZY,
    )
    var oauthConnections: MutableList<OAuthConnectionEntity> = mutableListOf(),
    @Version
    @Column(name = "version")
    var version: Long? = null,
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime,
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime,
) {
    /**
     * OAuthConnection 추가 헬퍼 메서드
     */
    fun addOAuthConnection(connection: OAuthConnectionEntity) {
        oauthConnections.add(connection)
        connection.member = this
    }

    /**
     * OAuthConnection 제거 헬퍼 메서드
     */
    fun removeOAuthConnection(connection: OAuthConnectionEntity) {
        oauthConnections.remove(connection)
        connection.member = null
    }

    /**
     * OAuthConnections 동기화 헬퍼 메서드
     */
    fun syncOAuthConnections(newConnections: List<OAuthConnectionEntity>) {
        // 기존 연결 중 새 목록에 없는 것 제거
        val newIds = newConnections.map { it.id }.toSet()
        val toRemove = oauthConnections.filter { it.id !in newIds }
        toRemove.forEach { removeOAuthConnection(it) }

        // 새 연결 중 기존에 없는 것 추가
        val existingIds = oauthConnections.map { it.id }.toSet()
        newConnections.filter { it.id !in existingIds }.forEach { addOAuthConnection(it) }
    }
}
