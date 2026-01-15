package io.clroot.selah.domains.member.application.service

import io.clroot.selah.common.application.afterCommit
import io.clroot.selah.common.application.publishAndClearEvents
import io.clroot.selah.domains.member.application.port.inbound.EmailVerificationUseCase
import io.clroot.selah.domains.member.application.port.inbound.SendVerificationEmailCommand
import io.clroot.selah.domains.member.application.port.inbound.VerifyEmailCommand
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenPort
import io.clroot.selah.domains.member.application.port.outbound.LoadMemberPort
import io.clroot.selah.domains.member.application.port.outbound.SaveMemberPort
import io.clroot.selah.domains.member.application.port.outbound.SendEmailPort
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.exception.EmailAlreadyVerifiedException
import io.clroot.selah.domains.member.domain.exception.EmailVerificationResendTooSoonException
import io.clroot.selah.domains.member.domain.exception.InvalidEmailVerificationTokenException
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.ApplicationEventPublisher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

/**
 * 이메일 인증 서비스
 */
@Service
@Transactional
class EmailVerificationService(
    private val loadMemberPort: LoadMemberPort,
    private val saveMemberPort: SaveMemberPort,
    private val emailVerificationTokenPort: EmailVerificationTokenPort,
    private val sendEmailPort: SendEmailPort,
    private val eventPublisher: ApplicationEventPublisher,
    private val applicationScope: CoroutineScope,
    @Value($$"${selah.email-verification.resend-cooldown:PT5M}")
    private val resendCooldown: Duration,
) : EmailVerificationUseCase {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun sendVerificationEmail(command: SendVerificationEmailCommand) {
        val member =
            loadMemberPort.findById(command.memberId)
                ?: throw MemberNotFoundException(command.memberId.value)

        // 이미 인증된 이메일인지 확인
        if (member.emailVerified) {
            throw EmailAlreadyVerifiedException(member.email.value)
        }

        val email = member.email
        val nickname = member.nickname

        // 재발송 쿨다운 확인
        checkResendCooldown(command.memberId)

        // 기존 토큰 무효화 및 새 토큰 생성
        emailVerificationTokenPort.invalidateAllByMemberId(command.memberId)
        val tokenResult = emailVerificationTokenPort.create(command.memberId)

        // 이메일 발송 (트랜잭션 커밋 후 비동기 처리)
        val verificationToken = tokenResult.rawToken
        afterCommit {
            applicationScope.launch {
                sendVerificationEmailAsync(email, nickname, verificationToken)
            }
        }
    }

    private suspend fun sendVerificationEmailAsync(
        email: Email,
        nickname: String,
        verificationToken: String,
    ) {
        try {
            sendEmailPort.sendVerificationEmail(
                to = email,
                nickname = nickname,
                verificationToken = verificationToken,
            )
            logger.info { "Verification email sent to ${email.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send verification email to ${email.value}" }
        }
    }

    override suspend fun verifyEmail(command: VerifyEmailCommand): Member {
        val tokenInfo =
            emailVerificationTokenPort.findValidByToken(command.token)
                ?: throw InvalidEmailVerificationTokenException()

        // 토큰 사용 처리
        emailVerificationTokenPort.markAsUsed(tokenInfo.id)

        // 회원 이메일 인증 처리
        val member =
            loadMemberPort.findById(tokenInfo.memberId)
                ?: throw MemberNotFoundException(tokenInfo.memberId.value)

        member.verifyEmail()
        val savedMember = saveMemberPort.save(member)
        savedMember.publishAndClearEvents(eventPublisher)

        logger.info { "Email verified for member ${member.id?.value}" }

        return savedMember
    }

    private suspend fun checkResendCooldown(memberId: MemberId) {
        val latestCreatedAt =
            emailVerificationTokenPort.findLatestCreatedAtByMemberId(memberId)
                ?: return

        val cooldownEnd = latestCreatedAt.plus(resendCooldown)
        val now = LocalDateTime.now()

        if (now.isBefore(cooldownEnd)) {
            val remainingSeconds = Duration.between(now, cooldownEnd).seconds
            throw EmailVerificationResendTooSoonException(remainingSeconds)
        }
    }
}
