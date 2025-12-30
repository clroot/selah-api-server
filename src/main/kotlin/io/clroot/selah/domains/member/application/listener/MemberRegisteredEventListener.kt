package io.clroot.selah.domains.member.application.listener

import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenPort
import io.clroot.selah.domains.member.application.port.outbound.SendEmailPort
import io.clroot.selah.domains.member.domain.event.MemberRegisteredEvent
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

/**
 * 회원 가입 이벤트 리스너
 *
 * 같은 도메인 내의 이벤트 처리이므로 Application 계층에 위치합니다.
 */
@Component
class MemberRegisteredEventListener(
    private val emailVerificationTokenPort: EmailVerificationTokenPort,
    private val sendEmailPort: SendEmailPort,
) {

    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    /**
     * 회원 가입 이벤트 처리
     *
     * 이메일 회원가입인 경우 (OAuth가 아닌 경우) 인증 이메일을 자동 발송합니다.
     * OAuth 가입은 emailVerified가 이미 true이므로 발송하지 않습니다.
     */
    @EventListener
    fun handle(event: MemberRegisteredEvent) {
        val member = event.member

        // OAuth 가입인 경우 (이미 이메일 인증됨) 스킵
        if (member.emailVerified) {
            logger.debug { "Skipping verification email for OAuth member: ${member.email.value}" }
            return
        }

        // 이메일 가입인 경우 인증 메일 발송
        runBlocking {
            try {
                val tokenResult = emailVerificationTokenPort.create(member.id)

                sendEmailPort.sendVerificationEmail(
                    to = member.email,
                    nickname = member.nickname,
                    verificationToken = tokenResult.rawToken,
                )

                logger.info { "Verification email sent to new member: ${member.email.value}" }
            } catch (e: Exception) {
                // 이메일 발송 실패해도 회원가입은 성공해야 함
                logger.error(e) { "Failed to send verification email to: ${member.email.value}" }
            }
        }
    }
}
