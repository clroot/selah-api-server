package io.clroot.selah.domains.member.adapter.outbound.persistence.serverkey

import org.springframework.data.jpa.repository.JpaRepository

interface ServerKeyJpaRepository : JpaRepository<ServerKeyEntity, String> {
    fun findByMemberId(memberId: String): ServerKeyEntity?

    fun existsByMemberId(memberId: String): Boolean

    fun deleteByMemberId(memberId: String)
}
