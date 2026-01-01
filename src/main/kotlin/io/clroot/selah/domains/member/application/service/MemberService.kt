package io.clroot.selah.domains.member.application.service

import io.clroot.selah.common.application.publishAndClearEvents
import io.clroot.selah.domains.member.application.port.inbound.GetCurrentMemberUseCase
import io.clroot.selah.domains.member.application.port.inbound.UpdateProfileCommand
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회원 정보 서비스
 */
@Service
@Transactional
class MemberService(
    private val loadMemberPort: LoadMemberPort,
    private val saveMemberPort: SaveMemberPort,
    private val eventPublisher: ApplicationEventPublisher,
) : GetCurrentMemberUseCase {
    @Transactional(readOnly = true)
    override suspend fun getMember(memberId: MemberId): Member =
        loadMemberPort.findById(memberId)
            ?: throw MemberNotFoundException(memberId.value)

    override suspend fun updateProfile(
        memberId: MemberId,
        command: UpdateProfileCommand,
    ): Member {
        val member =
            loadMemberPort.findById(memberId)
                ?: throw MemberNotFoundException(memberId.value)

        member.updateProfile(
            newNickname = command.nickname,
            newProfileImageUrl = command.profileImageUrl,
        )

        val savedMember = saveMemberPort.save(member)
        savedMember.publishAndClearEvents(eventPublisher)

        return savedMember
    }
}
