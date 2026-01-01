package io.clroot.selah.domains.member.adapter.outbound.email

import io.clroot.selah.domains.member.application.port.outbound.SendEmailPort
import io.clroot.selah.domains.member.domain.Email
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Component

/**
 * Spring Mail 기반 이메일 발송 Adapter
 */
@Component
class EmailSendAdapter(
    private val mailSender: JavaMailSender,
    @Value($$"${selah.email-verification.frontend-url:http://localhost:3000}")
    private val frontendUrl: String,
    @Value($$"${selah.email-verification.from-email:noreply@selah.io}")
    private val fromEmail: String,
    @Value($$"${selah.email-verification.from-name:Selah}")
    private val fromName: String,
) : SendEmailPort {
    companion object {
        @JvmStatic
        private val logger = KotlinLogging.logger {}
    }

    override suspend fun sendVerificationEmail(
        to: Email,
        nickname: String,
        verificationToken: String,
    ) {
        withContext(Dispatchers.IO) {
            val verificationUrl = "$frontendUrl/auth/verify-email?token=$verificationToken"

            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromEmail, fromName)
            helper.setTo(to.value)
            helper.setSubject("[Selah] 이메일 인증을 완료해주세요")
            helper.setText(buildEmailContent(nickname, verificationUrl), true)

            mailSender.send(message)

            logger.info { "Verification email sent to ${to.value}" }
        }
    }

    override suspend fun sendPasswordResetEmail(
        to: Email,
        nickname: String,
        resetToken: String,
    ) {
        withContext(Dispatchers.IO) {
            val resetUrl = "$frontendUrl/auth/reset-password?token=$resetToken"

            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromEmail, fromName)
            helper.setTo(to.value)
            helper.setSubject("[Selah] 비밀번호 재설정 안내")
            helper.setText(buildPasswordResetEmailContent(nickname, resetUrl), true)

            mailSender.send(message)

            logger.info { "Password reset email sent to ${to.value}" }
        }
    }

    override suspend fun sendPasswordChangedNotification(
        to: Email,
        nickname: String,
    ) {
        withContext(Dispatchers.IO) {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")

            helper.setFrom(fromEmail, fromName)
            helper.setTo(to.value)
            helper.setSubject("[Selah] 비밀번호가 변경되었습니다")
            helper.setText(buildPasswordChangedEmailContent(nickname), true)

            mailSender.send(message)

            logger.info { "Password changed notification sent to ${to.value}" }
        }
    }

    private fun buildEmailContent(
        nickname: String,
        verificationUrl: String,
    ): String =
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { text-align: center; color: #5C4A3D; margin-bottom: 30px; }
                .content { background: #FAF7F2; padding: 30px; border-radius: 8px; }
                .button {
                    display: inline-block;
                    background: #5C4A3D;
                    color: white !important;
                    padding: 12px 24px;
                    text-decoration: none;
                    border-radius: 6px;
                    margin: 20px 0;
                }
                .footer { color: #8B7355; font-size: 12px; margin-top: 30px; text-align: center; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Selah</h1>
                    <p>멈추고, 묵상하고, 기록하다</p>
                </div>
                <div class="content">
                    <h2>안녕하세요, ${nickname}님!</h2>
                    <p>Selah에 가입해 주셔서 감사합니다.</p>
                    <p>아래 버튼을 클릭하여 이메일 인증을 완료해주세요.</p>
                    <p style="text-align: center;">
                        <a href="$verificationUrl" class="button">이메일 인증하기</a>
                    </p>
                    <p style="font-size: 12px; color: #8B7355;">
                        버튼이 작동하지 않으면 아래 링크를 브라우저에 직접 붙여넣기 해주세요:<br>
                        <a href="$verificationUrl">$verificationUrl</a>
                    </p>
                    <p style="font-size: 12px; color: #8B7355;">
                        이 링크는 24시간 동안 유효합니다.
                    </p>
                </div>
                <div class="footer">
                    <p>본 메일은 발신 전용입니다.</p>
                    <p>Selah - 기도노트</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()

    private fun buildPasswordResetEmailContent(
        nickname: String,
        resetUrl: String,
    ): String =
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { text-align: center; color: #5C4A3D; margin-bottom: 30px; }
                .content { background: #FAF7F2; padding: 30px; border-radius: 8px; }
                .button {
                    display: inline-block;
                    background: #5C4A3D;
                    color: white !important;
                    padding: 12px 24px;
                    text-decoration: none;
                    border-radius: 6px;
                    margin: 20px 0;
                }
                .footer { color: #8B7355; font-size: 12px; margin-top: 30px; text-align: center; }
                .warning { color: #B91C1C; font-size: 12px; margin-top: 15px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Selah</h1>
                    <p>멈추고, 묵상하고, 기록하다</p>
                </div>
                <div class="content">
                    <h2>안녕하세요, ${nickname}님!</h2>
                    <p>비밀번호 재설정을 요청하셨습니다.</p>
                    <p>아래 버튼을 클릭하여 새 비밀번호를 설정해주세요.</p>
                    <p style="text-align: center;">
                        <a href="$resetUrl" class="button">비밀번호 재설정</a>
                    </p>
                    <p style="font-size: 12px; color: #8B7355;">
                        버튼이 작동하지 않으면 아래 링크를 브라우저에 직접 붙여넣기 해주세요:<br>
                        <a href="$resetUrl">$resetUrl</a>
                    </p>
                    <p style="font-size: 12px; color: #8B7355;">
                        이 링크는 1시간 동안 유효합니다.
                    </p>
                    <p class="warning">
                        비밀번호 재설정을 요청하지 않으셨다면, 이 이메일을 무시해주세요.
                    </p>
                </div>
                <div class="footer">
                    <p>본 메일은 발신 전용입니다.</p>
                    <p>Selah - 기도노트</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()

    private fun buildPasswordChangedEmailContent(nickname: String): String =
        """
        <!DOCTYPE html>
        <html>
        <head>
            <meta charset="UTF-8">
            <style>
                body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; }
                .container { max-width: 600px; margin: 0 auto; padding: 20px; }
                .header { text-align: center; color: #5C4A3D; margin-bottom: 30px; }
                .content { background: #FAF7F2; padding: 30px; border-radius: 8px; }
                .footer { color: #8B7355; font-size: 12px; margin-top: 30px; text-align: center; }
                .warning { color: #B91C1C; font-size: 12px; margin-top: 15px; padding: 10px; background: #FEF2F2; border-radius: 4px; }
            </style>
        </head>
        <body>
            <div class="container">
                <div class="header">
                    <h1>Selah</h1>
                    <p>멈추고, 묵상하고, 기록하다</p>
                </div>
                <div class="content">
                    <h2>안녕하세요, ${nickname}님!</h2>
                    <p>비밀번호가 성공적으로 변경되었습니다.</p>
                    <p>보안을 위해 모든 기기에서 로그아웃되었습니다.</p>
                    <p>새 비밀번호로 다시 로그인해주세요.</p>
                    <div class="warning">
                        <strong>본인이 변경하지 않으셨나요?</strong><br>
                        즉시 비밀번호를 재설정하시고, 계정 보안을 확인해주세요.
                    </div>
                </div>
                <div class="footer">
                    <p>본 메일은 발신 전용입니다.</p>
                    <p>Selah - 기도노트</p>
                </div>
            </div>
        </body>
        </html>
        """.trimIndent()
}
