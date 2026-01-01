package io.clroot.selah.domains.member.adapter.inbound.web

import io.clroot.selah.domains.member.adapter.inbound.security.SecurityUtils
import io.clroot.selah.domains.member.adapter.inbound.web.dto.EmailVerificationDto
import io.clroot.selah.domains.member.application.port.inbound.EmailVerificationUseCase
import io.clroot.selah.domains.member.application.port.inbound.SendVerificationEmailCommand
import io.clroot.selah.domains.member.application.port.inbound.VerifyEmailCommand
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.PasswordHash
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import org.springframework.http.HttpStatus
import java.time.LocalDateTime

class EmailVerificationControllerTest :
    DescribeSpec({

        val emailVerificationUseCase = mockk<EmailVerificationUseCase>()

        val controller =
            EmailVerificationController(
                emailVerificationUseCase = emailVerificationUseCase,
            )

        beforeTest {
            clearAllMocks()
            mockkObject(SecurityUtils)
        }

        afterTest {
            unmockkObject(SecurityUtils)
        }

        describe("sendVerificationEmail") {
            val memberId = MemberId.new()

            context("인증된 사용자가 요청할 때") {
                it("인증 이메일을 발송하고 200 OK를 반환한다") {
                    every { SecurityUtils.requireCurrentMemberId() } returns memberId
                    coEvery {
                        emailVerificationUseCase.sendVerificationEmail(
                            SendVerificationEmailCommand(memberId),
                        )
                    } just Runs

                    val response = controller.sendVerificationEmail()

                    response.statusCode shouldBe HttpStatus.OK
                    response.body?.data shouldBe Unit

                    coVerify(exactly = 1) {
                        emailVerificationUseCase.sendVerificationEmail(
                            SendVerificationEmailCommand(memberId),
                        )
                    }
                }
            }

            context("인증되지 않은 사용자가 요청할 때") {
                it("IllegalStateException이 발생한다") {
                    every { SecurityUtils.requireCurrentMemberId() } throws
                        IllegalStateException("No authenticated member found")

                    val exception =
                        runCatching {
                            controller.sendVerificationEmail()
                        }.exceptionOrNull()

                    exception shouldBe IllegalStateException("No authenticated member found")
                }
            }
        }

        describe("verifyEmail") {
            val memberId = MemberId.new()
            val email = Email("test@example.com")
            val token = "valid-verification-token"

            context("유효한 토큰으로 인증 요청할 때") {
                val verifiedMember = createVerifiedMember(id = memberId, email = email)
                val request = EmailVerificationDto.VerifyRequest(token = token)

                it("이메일 인증을 완료하고 회원 정보를 반환한다") {
                    coEvery {
                        emailVerificationUseCase.verifyEmail(VerifyEmailCommand(token))
                    } returns verifiedMember

                    val response = controller.verifyEmail(request)

                    response.statusCode shouldBe HttpStatus.OK
                    response.body?.data?.let { data ->
                        data.memberId shouldBe memberId.value
                        data.email shouldBe email.value
                        data.emailVerified shouldBe true
                    }

                    coVerify(exactly = 1) {
                        emailVerificationUseCase.verifyEmail(VerifyEmailCommand(token))
                    }
                }
            }
        }
    })

// region Test Fixtures

private fun createVerifiedMember(
    id: MemberId = MemberId.new(),
    email: Email = Email("test@example.com"),
    nickname: String = "TestUser",
): Member {
    val now = LocalDateTime.now()
    return Member(
        id = id,
        email = email,
        nickname = nickname,
        profileImageUrl = null,
        passwordHash = PasswordHash.from("${"$"}argon2id${"$"}v=19${"$"}m=65536,t=3,p=4${"$"}hashedvalue"),
        emailVerified = true,
        oauthConnections = emptyList(),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

// endregion
