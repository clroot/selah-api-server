package io.clroot.selah.domains.member.application.service

import io.clroot.selah.common.application.afterCommit
import io.clroot.selah.domains.member.application.port.inbound.*
import io.clroot.selah.domains.member.application.port.outbound.*
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.NewPassword
import io.clroot.selah.domains.member.domain.exception.InvalidPasswordResetTokenException
import io.clroot.selah.domains.member.domain.exception.MemberNotFoundException
import io.clroot.selah.domains.member.domain.exception.PasswordResetResendTooSoonException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.LocalDateTime

/**
 * 비밀번호 재설정 서비스
 */
@Service
@Transactional
class PasswordResetService(
    private val loadMemberPort: LoadMemberPort,
    private val saveMemberPort: SaveMemberPort,
    private val passwordResetTokenPort: PasswordResetTokenPort,
    private val passwordHashPort: PasswordHashPort,
    private val sessionPort: SessionPort,
    private val sendEmailPort: SendEmailPort,
    private val applicationScope: CoroutineScope,
    @Value($$"${selah.password-reset.resend-cooldown:PT1M}")
    private val resendCooldown: Duration,
) : PasswordResetUseCase {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun requestPasswordReset(command: RequestPasswordResetCommand) {
        val member = loadMemberPort.findByEmail(command.email)

        // 보안: 이메일 존재 여부를 노출하지 않음
        if (member == null) {
            logger.debug { "Password reset requested for non-existent email: ${command.email.value}" }
            return
        }

        val memberId = member.id
        val email = member.email
        val nickname = member.nickname

        // 재발송 쿨다운 확인
        checkResendCooldown(memberId)

        // 기존 토큰 무효화 및 새 토큰 생성
        passwordResetTokenPort.invalidateAllByMemberId(memberId)
        val tokenResult = passwordResetTokenPort.create(memberId)

        // 이메일 발송 (트랜잭션 커밋 후 비동기 처리)
        val resetToken = tokenResult.rawToken
        afterCommit {
            applicationScope.launch {
                sendPasswordResetEmailAsync(email, nickname, resetToken)
            }
        }
    }

    private suspend fun sendPasswordResetEmailAsync(
        email: Email,
        nickname: String,
        resetToken: String,
    ) {
        try {
            sendEmailPort.sendPasswordResetEmail(
                to = email,
                nickname = nickname,
                resetToken = resetToken,
            )
            logger.info { "Password reset email sent to ${email.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send password reset email to ${email.value}" }
        }
    }

    @Transactional(readOnly = true)
    override suspend fun validateResetToken(command: ValidateResetTokenCommand): ValidateResetTokenResult {
        val tokenInfo =
            passwordResetTokenPort.findValidByToken(command.token)
                ?: throw InvalidPasswordResetTokenException()

        val member =
            loadMemberPort.findById(tokenInfo.memberId)
                ?: throw MemberNotFoundException(tokenInfo.memberId.value)

        return ValidateResetTokenResult(
            valid = true,
            maskedEmail = maskEmail(member.email.value),
        )
    }

    override suspend fun resetPassword(command: ResetPasswordCommand) {
        val tokenInfo =
            passwordResetTokenPort.findValidByToken(command.token)
                ?: throw InvalidPasswordResetTokenException()

        // 비밀번호 정책 검증
        val newPassword = NewPassword.from(command.newPassword)

        // 회원 조회
        val member =
            loadMemberPort.findById(tokenInfo.memberId)
                ?: throw MemberNotFoundException(tokenInfo.memberId.value)

        val memberId = member.id
        val email = member.email
        val nickname = member.nickname

        // 비밀번호 해싱 및 변경
        val passwordHash = passwordHashPort.hash(newPassword)
        member.changePassword(passwordHash)
        saveMemberPort.save(member)

        // 토큰 사용 처리
        passwordResetTokenPort.markAsUsed(tokenInfo.id)

        // 모든 세션 무효화 (보안)
        sessionPort.deleteAllByMemberId(memberId)

        logger.info { "Password reset completed for member ${memberId.value}" }

        // 비밀번호 변경 알림 메일 발송 (트랜잭션 커밋 후 비동기 처리)
        afterCommit {
            applicationScope.launch {
                sendPasswordChangedNotificationAsync(email, nickname)
            }
        }
    }

    private suspend fun sendPasswordChangedNotificationAsync(
        email: Email,
        nickname: String,
    ) {
        try {
            sendEmailPort.sendPasswordChangedNotification(
                to = email,
                nickname = nickname,
            )
            logger.info { "Password changed notification sent to ${email.value}" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to send password changed notification to ${email.value}" }
        }
    }

    private suspend fun checkResendCooldown(memberId: MemberId) {
        val latestCreatedAt =
            passwordResetTokenPort.findLatestCreatedAtByMemberId(memberId)
                ?: return

        val cooldownEnd = latestCreatedAt.plus(resendCooldown)
        val now = LocalDateTime.now()

        if (now.isBefore(cooldownEnd)) {
            val remainingSeconds = Duration.between(now, cooldownEnd).seconds
            throw PasswordResetResendTooSoonException(remainingSeconds)
        }
    }

    private fun maskEmail(email: String): String {
        val atIndex = email.indexOf('@')
        if (atIndex <= 2) {
            return "*".repeat(atIndex) + email.substring(atIndex)
        }

        val visibleCount = 2
        val maskedPart = "*".repeat(atIndex - visibleCount)
        return email.substring(0, visibleCount) + maskedPart + email.substring(atIndex)
    }
}
