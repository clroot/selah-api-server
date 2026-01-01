package io.clroot.selah.domains.member.application.service

import io.clroot.selah.domains.member.application.port.inbound.ChangePasswordCommand
import io.clroot.selah.domains.member.application.port.inbound.ChangePasswordUseCase
import io.clroot.selah.domains.member.application.port.inbound.SetPasswordCommand
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.PasswordHashPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.application.port.outbound.SendEmailPort
import io.clroot.selah.domains.member.application.port.outbound.SessionPort
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.NewPassword
import io.clroot.selah.domains.member.domain.RawPassword
import io.clroot.selah.domains.member.domain.exception.InvalidCredentialsException
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import io.clroot.selah.domains.member.domain.exception.PasswordAlreadySetException
import io.clroot.selah.domains.member.domain.exception.PasswordNotSetException
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class PasswordService(
    private val loadMemberPort: LoadMemberPort,
    private val saveMemberPort: SaveMemberPort,
    private val passwordHashPort: PasswordHashPort,
    private val sessionPort: SessionPort,
    private val sendEmailPort: SendEmailPort,
) : ChangePasswordUseCase {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun changePassword(
        memberId: MemberId,
        command: ChangePasswordCommand,
    ) {
        val member =
            loadMemberPort.findById(memberId)
                ?: throw MemberNotFoundException(memberId.value)

        if (!member.hasPassword) {
            throw PasswordNotSetException()
        }

        val currentPassword = RawPassword(command.currentPassword)
        val isCurrentPasswordValid = passwordHashPort.verify(currentPassword, member.passwordHash!!)
        if (!isCurrentPasswordValid) {
            throw InvalidCredentialsException()
        }

        val newPassword = NewPassword.from(command.newPassword)
        val newPasswordHash = passwordHashPort.hash(newPassword)
        member.changePassword(newPasswordHash)
        saveMemberPort.save(member)

        sessionPort.deleteAllByMemberId(memberId)

        sendEmailPort.sendPasswordChangedNotification(
            to = member.email,
            nickname = member.nickname,
        )

        logger.info { "Password changed for member ${memberId.value}" }
    }

    override suspend fun setPassword(
        memberId: MemberId,
        command: SetPasswordCommand,
    ) {
        val member =
            loadMemberPort.findById(memberId)
                ?: throw MemberNotFoundException(memberId.value)

        if (member.hasPassword) {
            throw PasswordAlreadySetException()
        }

        val newPassword = NewPassword.from(command.newPassword)
        val newPasswordHash = passwordHashPort.hash(newPassword)
        member.setPassword(newPasswordHash)
        saveMemberPort.save(member)

        sendEmailPort.sendPasswordChangedNotification(
            to = member.email,
            nickname = member.nickname,
        )

        logger.info { "Password set for member ${memberId.value}" }
    }
}
