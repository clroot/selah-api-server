package io.clroot.selah.domains.member.adapter.outbound.persistence.member

import io.clroot.selah.domains.member.domain.OAuthProvider
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository

/**
 * Member JPA Repository
 */
@Repository
interface MemberJpaRepository : JpaRepository<MemberEntity, String> {
    /**
     * ID로 Member를 조회합니다. (OAuth 연결 포함)
     */
    @Query(
        """
        SELECT DISTINCT m FROM MemberEntity m
        LEFT JOIN FETCH m.oauthConnections
        WHERE m.id = :id
        """,
    )
    fun findByIdWithOAuthConnections(@Param("id") id: String): MemberEntity?

    /**
     * 이메일로 Member를 조회합니다. (OAuth 연결 포함)
     */
    @Query(
        """
        SELECT DISTINCT m FROM MemberEntity m
        LEFT JOIN FETCH m.oauthConnections
        WHERE m.email = :email
        """,
    )
    fun findByEmailWithOAuthConnections(@Param("email") email: String): MemberEntity?

    /**
     * 이메일 존재 여부를 확인합니다.
     */
    fun existsByEmail(email: String): Boolean

    /**
     * OAuth Provider와 Provider ID로 Member를 조회합니다. (OAuth 연결 포함)
     */
    @Query(
        """
        SELECT DISTINCT m FROM MemberEntity m
        LEFT JOIN FETCH m.oauthConnections
        WHERE EXISTS (
            SELECT 1 FROM OAuthConnectionEntity oc
            WHERE oc.member = m AND oc.provider = :provider AND oc.providerId = :providerId
        )
        """,
    )
    fun findByOAuthConnection(
        @Param("provider") provider: OAuthProvider,
        @Param("providerId") providerId: String,
    ): MemberEntity?
}
