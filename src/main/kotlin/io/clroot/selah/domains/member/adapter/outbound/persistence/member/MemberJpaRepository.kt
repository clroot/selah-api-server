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
     * 이메일로 Member를 조회합니다.
     */
    fun findByEmail(email: String): MemberEntity?

    /**
     * 이메일 존재 여부를 확인합니다.
     */
    fun existsByEmail(email: String): Boolean

    /**
     * OAuth Provider와 Provider ID로 Member를 조회합니다.
     */
    @Query(
        """
        SELECT m FROM MemberEntity m
        JOIN m.oauthConnections oc
        WHERE oc.provider = :provider AND oc.providerId = :providerId
        """,
    )
    fun findByOAuthConnection(
        @Param("provider") provider: OAuthProvider,
        @Param("providerId") providerId: String,
    ): MemberEntity?
}
