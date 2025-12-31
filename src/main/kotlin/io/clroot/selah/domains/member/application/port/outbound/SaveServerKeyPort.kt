package io.clroot.selah.domains.member.application.port.outbound

import io.clroot.selah.domains.member.domain.ServerKey

/**
 * Server Key 저장 Port
 */
interface SaveServerKeyPort {
    /**
     * Server Key 저장 (생성 또는 업데이트)
     */
    suspend fun save(serverKey: ServerKey): ServerKey
}
