package io.clroot.selah.domains.member.adapter.outbound.persistence.serverkey

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional

interface ServerKeyJpaRepository : JpaRepository<ServerKeyEntity, String> {
    fun findByMemberId(memberId: String): ServerKeyEntity?

    fun existsByMemberId(memberId: String): Boolean

    @Modifying
    @Transactional
    fun deleteByMemberId(memberId: String)
}
