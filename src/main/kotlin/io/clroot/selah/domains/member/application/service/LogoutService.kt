package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.LogoutUseCase
import io.clroot.selah.domains.member.application.port.outbound.SessionPort
import io.clroot.selah.domains.member.domain.MemberId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 로그아웃 서비스
 */
@Service
@Transactional
class LogoutService(
    private val sessionPort: SessionPort,
) : LogoutUseCase {
    override suspend fun logout(sessionToken: String) {
        sessionPort.delete(sessionToken)
    }

    override suspend fun logoutAll(memberId: MemberId) {
        sessionPort.deleteAllByMemberId(memberId)
    }
}
