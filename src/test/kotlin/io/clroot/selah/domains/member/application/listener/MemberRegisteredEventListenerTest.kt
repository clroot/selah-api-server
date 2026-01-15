package io.clroot.selah.domains.member.application.listener

import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenCreateResult
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenInfo
import io.clroot.selah.domains.member.application.port.outbound.EmailVerificationTokenPort
import io.clroot.selah.domains.member.application.port.outbound.SendEmailPort
import io.clroot.selah.domains.member.domain.Email
import io.clroot.selah.domains.member.domain.Member
import io.clroot.selah.domains.member.domain.MemberId
import io.clroot.selah.domains.member.domain.OAuthConnection
import io.clroot.selah.domains.member.domain.OAuthProvider
import io.clroot.selah.domains.member.domain.PasswordHash
import io.clroot.selah.domains.member.domain.event.MemberRegisteredEvent
import io.kotest.core.spec.style.DescribeSpec
import io.mockk.Runs
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import java.time.LocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class MemberRegisteredEventListenerTest :
    DescribeSpec({

        lateinit var emailVerificationTokenPort: EmailVerificationTokenPort
        lateinit var sendEmailPort: SendEmailPort
        lateinit var testScope: TestScope
        lateinit var listener: MemberRegisteredEventListener

        beforeEach {
            clearAllMocks()
            emailVerificationTokenPort = mockk()
            sendEmailPort = mockk()
            testScope = TestScope()
            listener =
                MemberRegisteredEventListener(
                    emailVerificationTokenPort = emailVerificationTokenPort,
                    sendEmailPort = sendEmailPort,
                    applicationScope = testScope,
                )
        }

        describe("handle") {
            context("이메일 가입 회원인 경우 (emailVerified = false)") {
                it("인증 이메일을 발송한다") {
                    val memberId = MemberId.new()
                    val email = Email("test@example.com")
                    val nickname = "TestUser"
                    val member =
                        createEmailMember(
                            id = memberId,
                            email = email,
                            nickname = nickname,
                            emailVerified = false,
                        )
                    val event = MemberRegisteredEvent(member)
                    val tokenResult = createTokenResult(memberId)

                    coEvery { emailVerificationTokenPort.create(memberId) } returns tokenResult
                    coEvery {
                        sendEmailPort.sendVerificationEmail(email, nickname, tokenResult.rawToken)
                    } just Runs

                    listener.handle(event)
                    testScope.advanceUntilIdle()

                    coVerify(exactly = 1) { emailVerificationTokenPort.create(memberId) }
                    coVerify(exactly = 1) {
                        sendEmailPort.sendVerificationEmail(email, nickname, tokenResult.rawToken)
                    }
                }
            }

            context("OAuth 가입 회원인 경우 (emailVerified = true)") {
                it("인증 이메일을 발송하지 않는다") {
                    val memberId = MemberId.new()
                    val email = Email("test@example.com")
                    val nickname = "TestUser"
                    val member =
                        createOAuthMember(
                            id = memberId,
                            email = email,
                            nickname = nickname,
                        )
                    val event = MemberRegisteredEvent(member)

                    listener.handle(event)
                    testScope.advanceUntilIdle()

                    coVerify(exactly = 0) { emailVerificationTokenPort.create(memberId) }
                    coVerify(exactly = 0) { sendEmailPort.sendVerificationEmail(email, nickname, any()) }
                }
            }

            context("이메일 발송이 실패하는 경우") {
                it("예외가 전파되지 않는다 (회원가입은 성공해야 함)") {
                    val memberId = MemberId.new()
                    val email = Email("test@example.com")
                    val nickname = "TestUser"
                    val member =
                        createEmailMember(
                            id = memberId,
                            email = email,
                            nickname = nickname,
                            emailVerified = false,
                        )
                    val event = MemberRegisteredEvent(member)
                    val tokenResult = createTokenResult(memberId)

                    coEvery { emailVerificationTokenPort.create(memberId) } returns tokenResult
                    coEvery {
                        sendEmailPort.sendVerificationEmail(email, nickname, tokenResult.rawToken)
                    } throws RuntimeException("SMTP connection failed")

                    // 예외가 전파되지 않아야 함
                    listener.handle(event)
                    testScope.advanceUntilIdle()

                    coVerify(exactly = 1) { emailVerificationTokenPort.create(memberId) }
                    coVerify(exactly = 1) {
                        sendEmailPort.sendVerificationEmail(email, nickname, tokenResult.rawToken)
                    }
                }
            }

            context("토큰 생성이 실패하는 경우") {
                it("예외가 전파되지 않는다 (회원가입은 성공해야 함)") {
                    val memberId = MemberId.new()
                    val email = Email("test@example.com")
                    val nickname = "TestUser"
                    val member =
                        createEmailMember(
                            id = memberId,
                            email = email,
                            nickname = nickname,
                            emailVerified = false,
                        )
                    val event = MemberRegisteredEvent(member)

                    coEvery {
                        emailVerificationTokenPort.create(memberId)
                    } throws RuntimeException("Database connection failed")

                    // 예외가 전파되지 않아야 함
                    listener.handle(event)
                    testScope.advanceUntilIdle()

                    coVerify(exactly = 1) { emailVerificationTokenPort.create(memberId) }
                    coVerify(exactly = 0) { sendEmailPort.sendVerificationEmail(email, nickname, any()) }
                }
            }
        }
    })

// region Test Fixtures

private fun createEmailMember(
    id: MemberId = MemberId.new(),
    email: Email = Email("test@example.com"),
    nickname: String = "TestUser",
    emailVerified: Boolean = false,
): Member {
    val now = LocalDateTime.now()
    return Member(
        id = id,
        email = email,
        nickname = nickname,
        profileImageUrl = null,
        passwordHash = PasswordHash.from("${"$"}argon2id${"$"}v=19${"$"}m=65536,t=3,p=4${"$"}hashedvalue"),
        emailVerified = emailVerified,
        oauthConnections = emptyList(),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

private fun createOAuthMember(
    id: MemberId = MemberId.new(),
    email: Email = Email("oauth@example.com"),
    nickname: String = "OAuthUser",
): Member {
    val now = LocalDateTime.now()
    return Member(
        id = id,
        email = email,
        nickname = nickname,
        profileImageUrl = "https://example.com/profile.jpg",
        passwordHash = null,
        emailVerified = true, // OAuth 회원은 이메일 인증됨
        oauthConnections =
            listOf(
                OAuthConnection.create(
                    provider = OAuthProvider.GOOGLE,
                    providerId = "google-id-123",
                ),
            ),
        role = Member.Role.USER,
        version = null,
        createdAt = now,
        updatedAt = now,
    )
}

private fun createTokenResult(memberId: MemberId): EmailVerificationTokenCreateResult {
    val now = LocalDateTime.now()
    return EmailVerificationTokenCreateResult(
        info =
            EmailVerificationTokenInfo(
                id = "token-id-${System.currentTimeMillis()}",
                memberId = memberId,
                expiresAt = now.plusDays(1),
                usedAt = null,
                createdAt = now,
            ),
        rawToken = "raw-token-${System.currentTimeMillis()}",
    )
}

// endregion
