package io.clroot.selah.domains.prayer.adapter.outbound.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface LookbackSelectionJpaRepository : JpaRepository<LookbackSelectionEntity, String>
