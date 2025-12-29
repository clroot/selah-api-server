package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import io.clroot.selah.domains.member.domain.*
import org.springframework.stereotype.Component

/**
 * Member Domain ↔ Entity 변환을 담당하는 Mapper
 *
 * Domain Layer와 Persistence Layer 간의 데이터 변환을 처리합니다.
 */
@Component
class MemberMapper {

    /**
     * Domain → Entity 변환
     */
    fun toEntity(member: Member): MemberEntity {
        val memberEntity = MemberEntity(
            id = member.id.value,
            email = member.email.value,
            nickname = member.nickname,
            profileImageUrl = member.profileImageUrl,
            passwordHash = member.passwordHash?.value,
            emailVerified = member.emailVerified,
            role = member.role,
            version = member.version,
            createdAt = member.createdAt,
            updatedAt = member.updatedAt,
        )

        // OAuthConnections 변환 및 연결
        member.oauthConnections.forEach { connection ->
            memberEntity.addOAuthConnection(toEntity(connection))
        }

        return memberEntity
    }

    /**
     * Entity → Domain 변환
     */
    fun toDomain(entity: MemberEntity): Member {
        return Member(
            id = MemberId.from(entity.id),
            email = Email(entity.email),
            nickname = entity.nickname,
            profileImageUrl = entity.profileImageUrl,
            passwordHash = entity.passwordHash?.let { PasswordHash.from(it) },
            emailVerified = entity.emailVerified,
            oauthConnections = entity.oauthConnections.map { toDomain(it) },
            role = entity.role,
            version = entity.version,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
        )
    }

    /**
     * OAuthConnection Domain → Entity 변환
     */
    fun toEntity(connection: OAuthConnection): OAuthConnectionEntity {
        return OAuthConnectionEntity(
            id = connection.id.value,
            provider = connection.provider,
            providerId = connection.providerId,
            connectedAt = connection.connectedAt,
        )
    }

    /**
     * OAuthConnection Entity → Domain 변환
     */
    fun toDomain(entity: OAuthConnectionEntity): OAuthConnection {
        return OAuthConnection(
            id = OAuthConnectionId.from(entity.id),
            provider = entity.provider,
            providerId = entity.providerId,
            connectedAt = entity.connectedAt,
        )
    }

    /**
     * 기존 Entity를 Domain 데이터로 업데이트
     * (ID, createdAt은 변경하지 않음)
     */
    fun updateEntity(entity: MemberEntity, member: Member) {
        entity.email = member.email.value
        entity.nickname = member.nickname
        entity.profileImageUrl = member.profileImageUrl
        entity.passwordHash = member.passwordHash?.value
        entity.emailVerified = member.emailVerified
        entity.role = member.role
        entity.updatedAt = member.updatedAt

        // OAuthConnections 동기화
        val newConnections = member.oauthConnections.map { toEntity(it) }
        entity.syncOAuthConnections(newConnections)
    }
}
