package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

interface LookbackSelectionJpaRepository : JpaRepository<LookbackSelectionEntity, String> {
    @Modifying
    @Transactional
    fun deleteByMemberIdAndSelectedAt(
        memberId: String,
        selectedAt: LocalDate,
    )
}
